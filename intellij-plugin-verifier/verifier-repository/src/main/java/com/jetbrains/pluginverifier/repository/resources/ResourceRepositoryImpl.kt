/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.repository.resources

import com.jetbrains.plugin.structure.base.utils.checkIfInterrupted
import com.jetbrains.plugin.structure.base.utils.closeOnException
import com.jetbrains.plugin.structure.base.utils.pluralize
import com.jetbrains.pluginverifier.repository.cleanup.UsageStatistic
import com.jetbrains.pluginverifier.repository.provider.ProvideResult
import com.jetbrains.pluginverifier.repository.provider.ResourceProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.Instant
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.FutureTask

/**
 * The implementation of the [resource repository] [ResourceRepository]
 * that can be safely used in a concurrent environment
 * where the resources can be added, accessed and removed by multiple threads.
 */
class ResourceRepositoryImpl<R, K, W : ResourceWeight<W>>(
  private val evictionPolicy: EvictionPolicy<R, K, W>,
  private val clock: Clock,
  private val resourceProvider: ResourceProvider<K, R>,
  initialWeight: W,
  weigher: (R) -> W,
  disposer: (R) -> Unit,
  private val presentableName: String = "ResourceRepository"
) : ResourceRepository<R, K, W> {
  private val logger: Logger = LoggerFactory.getLogger(presentableName)

  private val resourcesRegistrar = RepositoryResourcesRegistrar<R, K, W>(initialWeight, weigher, disposer, logger)

  private var nextLockId = 0L

  private val key2Locks = hashMapOf<K, MutableSet<ResourceLockImpl<R, K, W>>>()

  private val removeQueue = hashSetOf<K>()

  private val additionTasks = hashMapOf<K, FutureTask<ProvideResult<R>>>()

  private val additionWaitingThreads = hashMapOf<K, Int>()

  private val statistics = hashMapOf<K, UsageStatistic>()

  @Synchronized
  override fun add(key: K, resource: R) =
    try {
      addResource(key, resource)
    } finally {
      cleanup()
    }

  /**
   * Adds the [resource] to the [resourcesRegistrar].
   *
   * It doesn't invoke [cleanup] since this resource
   * may be awaited, which can lead to its eviction
   * and invalid resource locking.
   */
  @Synchronized
  private fun addResource(key: K, resource: R): Boolean {
    if (resourcesRegistrar.addResource(key, resource)) {
      check(key !in statistics)
      updateUsageStatistics(key)
      return true
    }
    return false
  }

  @Synchronized
  override fun getAllExistingKeys() = resourcesRegistrar.getAllKeys().toSet()

  @Synchronized
  override fun has(key: K) = resourcesRegistrar.has(key)

  @Synchronized
  override fun isLockedOrBeingProvided(key: K) = isLockedKey(key) || isBeingProvided(key)

  @Synchronized
  override fun remove(key: K): Boolean = when {
    isLockedOrBeingProvided(key) -> {
      logger.debugMaybe { "remove($key): the resource is locked or is being provided, enqueue for removing later." }
      removeQueue.add(key)
      false
    }
    resourcesRegistrar.has(key) -> {
      logger.debugMaybe { "remove($key): the resource is not locked, deleting now" }
      doRemove(key)
      cleanup()
      true
    }
    else -> false
  }

  @Synchronized
  override fun removeAll() {
    getAllExistingKeys().forEach { remove(it) }
  }

  @Synchronized
  private fun isLockedKey(key: K) = key2Locks.containsKey(key)

  @Synchronized
  private fun isBeingProvided(key: K) = additionTasks.containsKey(key)

  private fun updateUsageStatistics(key: K): Instant {
    val now = clock.instant()
    val usageStatistic = statistics.getOrPut(key) {
      UsageStatistic(now, 0)
    }
    usageStatistic.lastAccessTime = now
    usageStatistic.timesAccessed++
    return now
  }

  @Synchronized
  private fun registerLock(key: K): ResourceLock<R, W> {
    check(resourcesRegistrar.has(key))
    val resourceInfo = resourcesRegistrar.get(key)!!
    val now = updateUsageStatistics(key)
    val lockId = nextLockId++
    val lock = ResourceLockImpl(now, resourceInfo, key, lockId, this)
    logger.debugMaybe { "get($key): lock is registered $lock " }
    key2Locks.getOrPut(key) { hashSetOf() }.add(lock)
    return lock
  }

  @Synchronized
  internal fun releaseLock(lock: ResourceLockImpl<R, K, W>) {
    val key = lock.key
    val resourceLocks = key2Locks[key]
    if (resourceLocks != null) {
      logger.debugMaybe { "releasing lock $lock" }
      resourceLocks.remove(lock)
      if (resourceLocks.isEmpty()) {
        key2Locks.remove(key)

        if (key in removeQueue) {
          if (isBeingProvided(key)) {
            logger.debugMaybe { "hand over removing of the $key to another thread waiting for this key" }
          } else {
            logger.debugMaybe { "removing the $key as it is enqueued for removing and it has been just released" }
            removeQueue.remove(key)
            doRemove(key)
          }
        }
      }
    } else {
      logger.debugMaybe { "attempt to release an unregistered lock $lock" }
    }
  }

  @Synchronized
  private fun doRemove(key: K) {
    check(!isBeingProvided(key))
    resourcesRegistrar.removeResource(key)
    statistics.remove(key)
  }

  @Throws(InterruptedException::class)
  private fun getOrWait(key: K): ResourceRepositoryResult<R, W> {
    checkIfInterrupted()
    val (fetchTask, runInCurrentThread) = synchronized(this) {
      if (resourcesRegistrar.has(key)) {
        val lock = registerLock(key)
        logger.debugMaybe { "get($key): the resource is available and a lock is registered $lock" }
        return ResourceRepositoryResult.Found(lock)
      }

      val oldTask = additionTasks[key]
      additionWaitingThreads.compute(key) { _, v -> (v ?: 0) + 1 }
      if (oldTask != null) {
        logger.debugMaybe { "get($key): waiting for another thread to finish fetching the resource" }
        oldTask to false
      } else {
        logger.debugMaybe { "get($key): fetching the resource in the current thread" }
        val newTask = FutureTask {
          fetchAndAddResource(key)
        }
        additionTasks[key] = newTask
        newTask to true
      }
    }

    //Run the task in the current thread
    //if it started fetching the key first.
    if (runInCurrentThread) {
      fetchTask.run()
    }

    try {
      val provideResult = try {
        fetchTask.get() //propagate InterruptedException
      } catch (ce: CancellationException) {
        throw InterruptedException("Fetch task for $key has been cancelled")
      } catch (e: ExecutionException) {
        val cause = e.cause
        if (cause != null) {
          throw cause
        } else {
          throw RuntimeException("Failed to fetch result", e)
        }
      }
      return provideResult.registerLockIfProvided(key)
    } finally {
      synchronized(this) {
        additionWaitingThreads.compute(key) { _, v -> if (v!! == 1) null else (v - 1) }
        if (!additionWaitingThreads.containsKey(key)) {
          additionTasks.remove(key)
        }
      }
    }
  }

  private fun fetchAndAddResource(key: K): ProvideResult<R> {
    val provideResult = resourceProvider.provide(key)
    if (provideResult is ProvideResult.Provided<R>) {
      addResource(key, provideResult.resource)
    }
    return provideResult
  }

  private fun ProvideResult<R>.registerLockIfProvided(key: K) = when (this) {
    is ProvideResult.Provided<R> -> ResourceRepositoryResult.Found(registerLock(key))
    is ProvideResult.NotFound<R> -> ResourceRepositoryResult.NotFound<R, W>(reason)
    is ProvideResult.Failed<R> -> ResourceRepositoryResult.Failed(reason, error)
  }

  @Synchronized
  override fun getAvailableResources() =
    resourcesRegistrar.resources.map { (key, resourceInfo) ->
      AvailableResource(key, resourceInfo, statistics[key]!!, isLockedKey(key))
    }

  @Synchronized
  override fun cleanup() {
    if (evictionPolicy.isNecessary(resourcesRegistrar.totalWeight)) {
      val availableResources = resourcesRegistrar.entries.map { (key, resourceInfo) ->
        AvailableResource(key, resourceInfo, statistics[key]!!, isLockedKey(key))
      }

      val evictionInfo = EvictionInfo(resourcesRegistrar.totalWeight, availableResources)
      val resourcesForEviction = evictionPolicy.selectResourcesForEviction(evictionInfo)

      if (resourcesForEviction.isNotEmpty()) {
        val disposedTotalWeight = resourcesForEviction.map { it.resourceInfo.weight }.reduce { acc, weight -> acc + weight }
        logger.debugMaybe {
          "It's time to evict unused resources. " +
            "Total weight: ${resourcesRegistrar.totalWeight}. " +
            "${resourcesForEviction.size} " + "resource".pluralize(resourcesForEviction.size) +
            " will be evicted with total weight $disposedTotalWeight"
        }
        for (resource in resourcesForEviction) {
          remove(resource.key)
        }
      }
    }
  }

  /**
   * Provides the resource by [key].
   *
   * If the resource is cached, returns it from the cache,
   * otherwise it firstly provides the resource, adds it to
   * to the cache and returns it.

   * The possible results are represented as subclasses of [ResourceRepositoryResult].
   * If the resource is available in the cache or successfully provided, the resource lock is registered
   * for the resource so it will be protected against deletions by other threads.
   *
   * This method is thread safe. In case several threads attempt to get the same resource, only one
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
      cleanup()
    }
    return result
  }

  override fun toString() = presentableName

}