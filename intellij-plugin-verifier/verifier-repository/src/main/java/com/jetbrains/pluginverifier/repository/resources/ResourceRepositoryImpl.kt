/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.repository.resources

import com.jetbrains.plugin.structure.base.utils.checkIfInterrupted
import com.jetbrains.plugin.structure.base.utils.closeOnException
import com.jetbrains.plugin.structure.base.utils.pluralize
import com.jetbrains.plugin.structure.base.utils.rethrowIfInterrupted
import com.jetbrains.pluginverifier.repository.cleanup.UsageStatistic
import com.jetbrains.pluginverifier.repository.provider.ProvideResult
import com.jetbrains.pluginverifier.repository.provider.ResourceProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Clock
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * The implementation of the [resource repository][ResourceRepository]
 * that can be safely used in a concurrent environment
 * where the resources can be added, accessed, and removed by multiple threads.
 *
 * Data structure that maintains a set of registered resources and their total weights.
 *
 * It is initialized with initial weight of [totalWeight],
 * typically equal zero in the units of chosen weights domain, the [weigher] used to assign
 * weights of the resources in a controlled way and the [disposer] used to deallocate
 * the resources being removed.
 */
class ResourceRepositoryImpl<R : Any, K : Any, W : ResourceWeight<W>>(
  private val evictionPolicy: EvictionPolicy<R, K, W>,
  private val clock: Clock,
  private val resourceProvider: ResourceProvider<K, R>,
  initialWeight: W,
  private val weigher: (R) -> W,
  private val disposer: (R) -> Unit,
  private val presentableName: String = "ResourceRepository"
) : ResourceRepository<R, K, W> {
  private val logger: Logger = LoggerFactory.getLogger(presentableName)

  private val nextLockId = AtomicLong()

  private val storage = ConcurrentHashMap<K, StorageStatus>()
  private val totalWeight: AtomicReference<W> = AtomicReference(initialWeight)
  private val removeQueue: MutableSet<Pair<K, StorageStatus>> = ConcurrentHashMap.newKeySet()

  sealed interface StorageStatus

  @Suppress("EqualsOrHashCode")
  class Fetching<R : Any>(callable: Callable<ProvideResult<R>>) : StorageStatus, FutureTask<ProvideResult<R>>(callable) {
    @Volatile
    var fetched: Stored<R, *, *>? = null

    override fun equals(other: Any?): Boolean {
      return this === other
    }
  }

  @Suppress("EqualsOrHashCode")
  class Stored<R : Any, K : Any, W : ResourceWeight<W>>(
    val info: ResourceInfo<R, W>,
    val statistic: UsageStatistic,
  ) : StorageStatus {
    internal val locks = HashSet<ResourceLockImpl<R, K, W>>()

    @Volatile
    internal var removed = false // guarded by `locks`

    /**
     * Use identity equals
     */
    override fun equals(other: Any?): Boolean {
      return this === other
    }
  }

  override fun add(key: K, resource: R) =
    try {
      val weight = weigher(resource)
      val previous = storage.putIfAbsent(key, Stored<R, K, W>(ResourceInfo(resource, weight), UsageStatistic(clock.instant(), 1)))
      val added = previous === null
      if (added) {
        totalWeight.accumulateAndGet(weight) { acc, weight -> acc + weight }
      }
      added
    } finally {
      maybeCleanup()
    }

  override fun getAllExistingKeys(): Set<K> = HashSet(storage.keys)

  override fun has(key: K): Boolean = storage.containsKey(key)

  override fun isLockedOrBeingProvided(key: K): Boolean {
    val value = storage[key] ?: return false
    when (value) {
      is Fetching<*> -> return true
      is Stored<*, *, *> -> synchronized(value.locks) { return value.locks.isNotEmpty() }
    }
  }

  override fun remove(key: K): Boolean = remove2(key, true)

  private fun remove2(key: K, cleanupIfRemoved: Boolean): Boolean {
    val value = storage[key] ?: return false
    when (value) {
      is Fetching<*> -> {
        logger.debugMaybe { "remove($key): the resource is being provided, enqueue for removing later" }
        removeQueue.add(key to (value.fetched ?: value))
        return false
      }

      is Stored<*, *, *> -> {
        val removed = synchronized(value.locks) {
          if (value.locks.isNotEmpty()) {
            logger.debugMaybe { "remove($key): the resource is locked, enqueue for removing later" }
            removeQueue.add(key to value)
            return false
          }
          logger.debugMaybe { "remove($key): the resource is not locked, deleting now" }
          value.removed = true
          storage.remove(key, value)
        }

        if (removed) {
          @Suppress("UNCHECKED_CAST")
          val info = value.info as ResourceInfo<*, W>
          totalWeight.accumulateAndGet(info.weight) { acc, weight -> acc - weight }
          @Suppress("UNCHECKED_CAST")
          safeDispose(key, value.info.resource as R)
        } else {
          // Association has changed, probably some other thread removed it and optionally put another value.
          // Since our `value` is obsolete and has no locks, consider that it's removed.
        }
        if (cleanupIfRemoved) {
          maybeCleanup()
        }
        return true
      }
    }
  }

  override fun removeAll() {
    if (getAllExistingKeys().map { remove2(it, false) }.any { it }) {
      cleanup()
    }
  }

  internal fun releaseLock(lock: ResourceLockImpl<R, K, W>) {
    logger.debugMaybe { "releasing lock $lock" }

    val key = lock.key
    val value = lock.value
    val pair = key to value

    val hasLocks = synchronized(value.locks) {
      value.locks.remove(lock)
      value.locks.isNotEmpty()
    }

    if (hasLocks) {
      return
    }

    refreshRemoveQueue()

    // Probably should be removed since no more locks left
    if (removeQueue.remove(pair)) {
      // was in the queue, remove it from the storage if there are no locks
      val removed = synchronized(value.locks) {
        if (value.locks.isNotEmpty()) {
          // someone acquired a lock while we were removing the pair from the queue, put it back
          removeQueue.add(pair)
          return
        }
        value.removed = true
        storage.remove(key, value)
      }

      if (removed) {
        totalWeight.accumulateAndGet(value.info.weight) { acc, weight -> acc - weight }
        safeDispose(key, value.info.resource)
      } else {
        // Association has changed, probably some other thread removed it and optionally put another value.
        // Since our `value` is obsolete and has no locks, consider that it's removed.
      }
    }
  }

  private fun refreshRemoveQueue() {
    for (pair in removeQueue) {
      if (pair.second is Fetching<*>) {
        val fetched = (pair.second as Fetching<*>).fetched
        if (fetched != null) {
          removeQueue.remove(pair)
          removeQueue.add(pair.first to fetched)
        }
      }
    }
  }

  @Throws(InterruptedException::class)
  private fun getOrWait(key: K): ResourceRepositoryResult<R, W> {
    while (true) {
      checkIfInterrupted()

      val value: StorageStatus? = storage[key]
      if (value is Stored<*, *, *>) {
        val now = clock.instant()
        val lockId = nextLockId.incrementAndGet()

        @Suppress("UNCHECKED_CAST")
        val lock = ResourceLockImpl(now, key, lockId, this, value as Stored<R, K, W>)
        // attempt to lock if `value` is not removed yet

        val removed = synchronized(value.locks) {
          if (value.removed) {
            // was marked as removed, can no longer add any lock, re-run the whole method to read new association from storage
          } else {
            // implies that `storage[key] === value`, otherwise `value.removed` would be `true.`
            value.statistic.access(now)
            value.locks.add(lock)
          }
          value.removed
        }
        if (removed) {
          continue
        }
        logger.debugMaybe { "get($key): the resource is available and a lock is registered $lock" }
        return ResourceRepositoryResult.Found(lock)
      }
      val fetchTask: Fetching<R>
      val runInCurrentThread: Boolean
      if (value is Fetching<*>) {
        logger.debugMaybe { "get($key): waiting for another thread to finish fetching the resource" }
        @Suppress("UNCHECKED_CAST")
        fetchTask = value as Fetching<R>
        runInCurrentThread = false
      } else {
        assert(value === null)
        logger.debugMaybe { "get($key): fetching the resource in the current thread" }
        val newTask = Fetching {
          fetchResource(key)
        }
        if (storage.putIfAbsent(key, newTask) != null) {
          // value has changed, re-run the whole method
          continue
        }
        fetchTask = newTask
        runInCurrentThread = true
      }

      // Run the task in the current thread if it started fetching the key first.
      if (runInCurrentThread) {
        fetchTask.run()
      }

      val provideResult = try {
        fetchTask.get() //propagate InterruptedException
      } catch (_: CancellationException) {
        throw InterruptedException("Fetch task for $key has been cancelled")
      } catch (e: ExecutionException) {
        val cause = e.cause
        if (cause != null) {
          throw cause
        } else {
          throw RuntimeException("Failed to fetch result", e)
        }
      }

      // the first-possible thread should update storage
      if (provideResult is ProvideResult.Provided<R>) {
        val created = Stored<R, K, W>(ResourceInfo(provideResult.resource, weigher(provideResult.resource)), UsageStatistic(clock.instant(), 1))
        if (storage.replace(key, fetchTask, created)) {
          // successfully replaced the task with the created value
          totalWeight.accumulateAndGet(created.info.weight) { acc, weight -> acc + weight }
          fetchTask.fetched = created
          if (removeQueue.remove(key to fetchTask)) {
            removeQueue.add(key to created)
          }
        } else {
          // probably another thread updated the association, re-run the whole method
          continue
        }
        // re-run the whole method, it will return 'Found' resource
        continue
      } else {
        // remove unsuccessful task from the storage
        storage.remove(key, fetchTask)
        return when (provideResult) {
          is ProvideResult.NotFound<R> -> ResourceRepositoryResult.NotFound(provideResult.reason)
          is ProvideResult.Failed<R> -> ResourceRepositoryResult.Failed(provideResult.reason, provideResult.error)
          else -> throw IllegalStateException("Unexpected result type: $provideResult")
        }
      }
    }
  }

  private fun fetchResource(key: K): ProvideResult<R> {
    val provideResult = resourceProvider.provide(key)
    return provideResult
  }

  override fun getAvailableResources(): List<AvailableResource<R, K, W>> {
    return storage.entries.mapNotNull {
      if (it.value is Fetching<*>) return@mapNotNull null
      @Suppress("UNCHECKED_CAST")
      val value = it.value as Stored<R, K, W>
      // copying statistics since it could be used in sorting and it should be unmodifiable
      val (isLocked, stats) = synchronized(value.locks) { value.locks.isNotEmpty() to value.statistic.copy() }
      AvailableResource(it.key, value.info, stats, isLocked)
    }
  }

  override fun cleanup() {
    if (maybeCleanup()) {
      return
    }
    // wait for another running cleanup, run one more from the current thread
    var skipped = 0
    while (true) {
      val stop = cleanupState.lock.withLock {
        if (cleanupState.running) {
          cleanupState.condition.await()
        }
        if (!cleanupState.running) {
          cleanupState.running = true
          skipped = cleanupState.skipped
          true
        } else {
          false
        }
      }
      if (stop) {
        break
      }
    }
    try {
      doCleanup()
    } finally {
      cleanupState.lock.withLock {
        cleanupState.running = false
        cleanupState.skipped -= skipped
        cleanupState.condition.signalAll()
      }
    }
  }

  /**
   * Runs cleanup if other thread isn't doing it, else skip
   *
   * @return whether cleanup was performed by the current thread
   */
  fun maybeCleanup(): Boolean {
    val skipped: Int = cleanupState.lock.withLock {
      if (!cleanupState.running) {
        cleanupState.running = true
        cleanupState.skipped
      } else {
        cleanupState.skipped++
        return false
      }
    }
    try {
      doCleanup()
    } finally {
      cleanupState.lock.withLock {
        cleanupState.running = false
        cleanupState.skipped -= skipped
        cleanupState.condition.signalAll()
      }
    }
    return true
  }

  private class CleanupState {
    val lock = ReentrantLock()
    val condition: Condition = lock.newCondition()
    var running: Boolean = false
    var skipped: Int = 0
  }

  private val cleanupState = CleanupState()

  fun doCleanup() {
    if (evictionPolicy.isNecessary(totalWeight.get())) {
      val availableResources = getAvailableResources()
      val totalWeight = availableResources.map { it.resourceInfo.weight }.reduce { acc, weight -> acc + weight }

      val evictionInfo = EvictionInfo(totalWeight, availableResources)
      val resourcesForEviction = evictionPolicy.selectResourcesForEviction(evictionInfo)

      if (resourcesForEviction.isNotEmpty()) {
        val disposedTotalWeight = resourcesForEviction.map { it.resourceInfo.weight }.reduce { acc, weight -> acc + weight }
        logger.debugMaybe {
          "It's time to evict unused resources. " +
            "Total weight: $totalWeight. " +
            "${resourcesForEviction.size} " + "resource".pluralize(resourcesForEviction.size) +
            " will be evicted with total weight $disposedTotalWeight"
        }
        for (resource in resourcesForEviction) {
          remove2(resource.key, false)
        }
      }
    }
  }

  private fun safeDispose(key: K, resource: R) {
    try {
      logger.debugMaybe { "dispose($key)" }
      disposer(resource)
    } catch (e: Exception) {
      e.rethrowIfInterrupted()
      logger.error("unable to dispose the resource $resource", e)
    }
  }


  /**
   * Provides the resource by [key].
   *
   * If the resource is cached, returns it from the cache,
   * otherwise it firstly provides the resource, adds it
   * to the cache, and returns it.

   * The possible results are represented as subclasses of [ResourceRepositoryResult].
   * If the resource is available in the cache or successfully provided, the resource lock is registered
   * for the resource, so it will be protected against deletions by other threads.
   *
   * This method is thread-safe. In case several threads attempt to get the same resource, only one
   * of them provides the resource while others wait for the first to complete and return the same resource.
   *
   * @throws InterruptedException if the current thread has been interrupted while waiting for the resource.
   */
  @Throws(InterruptedException::class)
  override fun get(key: K): ResourceRepositoryResult<R, W> {
    val result = getOrWait(key)
    /**
     * Release the lock if the cleanup procedure has failed.
     */
    (result as? ResourceRepositoryResult.Found<*, *>)?.lockedResource?.closeOnException {
      maybeCleanup()
    }
    return result
  }

  override fun toString() = presentableName

}