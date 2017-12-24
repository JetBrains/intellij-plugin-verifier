package com.jetbrains.pluginverifier.repository.resources

import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.jetbrains.pluginverifier.misc.pluralize
import com.jetbrains.pluginverifier.repository.cleanup.UsageStatistic
import com.jetbrains.pluginverifier.repository.provider.ProvideResult
import com.jetbrains.pluginverifier.repository.provider.ResourceProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.Executors
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit

/**
 * The implementation of the [resource repository] [ResourceRepository]
 * that can be safely used in a concurrent environment
 * where the resources can be added, accessed and removed by multiple threads.
 */
class ResourceRepositoryImpl<R, K>(private val evictionPolicy: EvictionPolicy<R, K>,
                                   private val clock: Clock,
                                   private val resourceProvider: ResourceProvider<K, R>,
                                   initialWeight: ResourceWeight,
                                   weigher: (R) -> ResourceWeight,
                                   disposer: (R) -> Unit) : ResourceRepository<R, K> {

  companion object {
    val LOG: Logger = LoggerFactory.getLogger(ResourceRepositoryImpl::class.java)

    private val LOCK_TIME_TO_LIVE_DURATION: Duration = Duration.of(1, ChronoUnit.HOURS)
  }

  private val resourcesRegistrar = RepositoryResourcesRegistrar<R, K>(initialWeight, weigher, disposer)

  private var nextLockId: Long = 0

  private val key2Locks = hashMapOf<K, MutableSet<ResourceLock<R>>>()

  private val deleteQueue = hashSetOf<K>()

  private val waitedKeys = hashSetOf<K>()

  private val provisionTasks = hashMapOf<K, FutureTask<ProvideResult<R>>>()

  private val statistics = hashMapOf<K, UsageStatistic>()

  init {
    runForgottenLocksInspector()
  }

  private fun runForgottenLocksInspector() {
    Executors.newSingleThreadScheduledExecutor(
        ThreadFactoryBuilder()
            .setDaemon(true)
            .build()
    ).scheduleAtFixedRate({ detectForgottenLocks() }, 1, 60, TimeUnit.MINUTES)
  }

  @Synchronized
  override fun add(key: K, resource: R): Boolean {
    if (resourcesRegistrar.has(key)) {
      return false
    }
    assert(key !in statistics)
    resourcesRegistrar.addResource(key, resource)
    statistics[key] = UsageStatistic(Instant.EPOCH, 0)
    cleanup()
    return true
  }

  @Synchronized
  override fun <R> lockAndExecute(block: () -> R): R = block()

  @Synchronized
  override fun getAllExistingKeys() = resourcesRegistrar.getAllKeys().toSet()

  @Synchronized
  override fun has(key: K) = resourcesRegistrar.has(key)

  @Synchronized
  override fun remove(key: K): Boolean = if (isLockedKey(key) || isBeingProvided(key)) {
    LOG.debug("Deletion of $key: the resource is locked or is being provided, delete later.")
    deleteQueue.add(key)
    false
  } else if (resourcesRegistrar.has(key)) {
    LOG.debug("Deletion of $key: non-locked, delete now")
    doRemove(key)
    cleanup()
    true
  } else {
    false
  }

  @Synchronized
  override fun removeAll() {
    getAllExistingKeys().forEach { remove(it) }
  }

  @Synchronized
  private fun isLockedKey(key: K) = key2Locks.containsKey(key)

  @Synchronized
  private fun isBeingProvided(key: K) = key in waitedKeys

  @Synchronized
  private fun registerLock(key: K): ResourceLockImpl<R, K> {
    assert(resourcesRegistrar.has(key))
    val resourceInfo = resourcesRegistrar.get(key)!!
    val lockTime = clock.instant()
    val lock = ResourceLockImpl(resourceInfo.resource, lockTime, key, nextLockId++, this)
    key2Locks.getOrPut(key, { hashSetOf() }).add(lock)

    val keyUsageStatistic = statistics.getOrPut(key, { UsageStatistic(lockTime, 0) })
    keyUsageStatistic.timesAccessed++
    return lock
  }

  @Synchronized
  internal fun releaseLock(lock: ResourceLockImpl<R, K>) {
    val key = lock.key
    val resourceLocks = key2Locks[key]
    if (resourceLocks != null) {
      resourceLocks.remove(lock)
      if (resourceLocks.isEmpty()) {
        key2Locks.remove(key)
      }
    }
    if (key in deleteQueue) {
      deleteQueue.remove(key)
      doRemove(key)
    }
  }

  @Synchronized
  private fun doRemove(key: K) {
    assert(key !in provisionTasks)
    assert(key !in waitedKeys)
    resourcesRegistrar.deleteResource(key)
    statistics.remove(key)
  }

  private fun provideOrWait(key: K): ResourceRepositoryResult<R> {
    val (provideTask, runInCurrentThread) = synchronized(this) {
      waitedKeys.add(key)
      val task = provisionTasks[key]
      if (task != null) {
        task to false
      } else {
        val provideTask = FutureTask {
          provideAndAddResource(key)
        }
        provisionTasks[key] = provideTask
        provideTask to true
      }
    }

    //Run the provision task if the current thread has initialized it.
    if (runInCurrentThread) {
      provideTask.run()
    }

    try {
      val provideResult = provideTask.get()
      return provideResult.registerLockIfProvided(key)
    } finally {
      synchronized(this) {
        waitedKeys.remove(key)

        if (runInCurrentThread) {
          provisionTasks.remove(key)
        }
      }
    }
  }

  private fun provideAndAddResource(key: K): ProvideResult<R> {
    val provideResult = resourceProvider.provide(key)
    if (provideResult is ProvideResult.Provided<R>) {
      add(key, provideResult.resource)
    }
    return provideResult
  }

  private fun ProvideResult<R>.registerLockIfProvided(key: K) = when (this) {
    is ProvideResult.Provided<R> -> ResourceRepositoryResult.Found(registerLock(key))
    is ProvideResult.NotFound<R> -> ResourceRepositoryResult.NotFound<R>(reason)
    is ProvideResult.Failed<R> -> ResourceRepositoryResult.Failed(reason, error)
  }

  @Synchronized
  private fun lockResourceIfExists(key: K): ResourceLockImpl<R, K>? {
    if (resourcesRegistrar.has(key)) {
      return registerLock(key)
    }
    return null
  }

  @Synchronized
  private fun detectForgottenLocks() {
    for ((key, locks) in key2Locks) {
      for (lock in locks) {
        val now = clock.instant()
        val lockTime = lock.lockTime
        val maxUnlockTime = lockTime.plus(LOCK_TIME_TO_LIVE_DURATION)
        val isForgotten = now.isAfter(maxUnlockTime)
        if (isForgotten) {
          LOG.warn("Forgotten lock found for $key on ${lock.resource}; lock time = $lockTime")
        }
      }
    }
  }

  @Synchronized
  override fun cleanup() {
    if (evictionPolicy.isNecessary(resourcesRegistrar.totalWeight)) {
      val availableResources = resourcesRegistrar.resources.map { (key, resourceInfo) ->
        AvailableResource(key, resourceInfo, statistics[key]!!, isLockedKey(key))
      }

      val evictionInfo = EvictionInfo(resourcesRegistrar.totalWeight, availableResources)
      val resourcesForDisposition = evictionPolicy.selectResourcesForDeletion(evictionInfo)

      if (resourcesForDisposition.isNotEmpty()) {
        val disposedTotalWeight = resourcesForDisposition.map { it.resourceInfo.weight }.reduce { acc, weight -> acc + weight }
        LOG.info("It's time to dispose unused resources. " +
            "Total weight: ${resourcesRegistrar.totalWeight}. " +
            "${resourcesForDisposition.size} " + "resource".pluralize(resourcesForDisposition.size) +
            " will be removed having total size $disposedTotalWeight"
        )
        for (availableFile in resourcesForDisposition) {
          remove(availableFile.key)
        }
      }
    }
  }

  /**
   * Provides the resource by [key].
   *
   * If the resource is cached, returns it from the cache,
   * otherwise it firstly provides the resources, adds the resource
   * to the cache and returns it.

   * The possible results are represented as subclasses of [ResourceRepositoryResult].
   * If the resource is available in the cache or successfully provided, the resource lock is registered
   * for the resource so it will be protected against deletions by other threads.
   *
   * This method is thread safe. In case several threads attempt to get the same resource, only one
   * of them provides the resource while others wait for the first to complete and return the same resource.
   */
  override fun get(key: K): ResourceRepositoryResult<R> {
    val lockedResource = lockResourceIfExists(key)
    val result = if (lockedResource != null) {
      ResourceRepositoryResult.Found(lockedResource)
    } else {
      provideOrWait(key)
    }
    try {
      cleanup()
    } catch (e: Throwable) {
      /**
       * Release the lock if the cleanup procedure has failed.
       */
      (result as? ResourceRepositoryResult.Found<*>)?.lockedResource?.release()
      throw e
    }
    return result
  }
}