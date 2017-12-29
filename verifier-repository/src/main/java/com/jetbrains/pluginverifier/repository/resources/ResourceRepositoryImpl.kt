package com.jetbrains.pluginverifier.repository.resources

import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.jetbrains.pluginverifier.misc.checkIfInterrupted
import com.jetbrains.pluginverifier.misc.closeOnException
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
                                   disposer: (R) -> Unit,
                                   private val presentableName: String = "ResourceRepository",
                                   /**
                                    * This optional parameter is used to specify the maximum expected
                                    * time of a resource lock being in the locked state.
                                    *
                                    * It is only used to monitor
                                    * the unreleased locks and emit a warning if
                                    * there are long-living locks found which
                                    * typically means that those locks are not
                                    * released due to a programming bug.
                                    *
                                    * If this parameter is set to `null`, no warnings will be emitted.
                                    * Note that long-living locks are fine in some cases, like locks
                                    * for the [resource cache entries] [com.jetbrains.pluginverifier.repository.cache.ResourceCache]
                                    */
                                   private val expectedMaximumLockTime: Duration? = Duration.of(1, ChronoUnit.HOURS)) : ResourceRepository<R, K> {

  companion object {
    val LOG: Logger = LoggerFactory.getLogger(ResourceRepositoryImpl::class.java)
  }

  private val resourcesRegistrar = RepositoryResourcesRegistrar<R, K>(initialWeight, weigher, disposer)

  private var nextLockId: Long = 0

  private val key2Locks = hashMapOf<K, MutableSet<ResourceLock<R>>>()

  private val deleteQueue = hashSetOf<K>()

  private val waitedKeys = hashSetOf<K>()

  private val provisionTasks = hashMapOf<K, FutureTask<ProvideResult<R>>>()

  private val statistics = hashMapOf<K, UsageStatistic>()

  init {
    if (expectedMaximumLockTime != null) {
      runForgottenLocksInspector()
    }
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
    resourcesRegistrar.addResource(key, resource)
    assert(key !in statistics)
    updateUsageStatistics(key)
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
  private fun registerLock(key: K): ResourceLockImpl<R, K> {
    assert(resourcesRegistrar.has(key))
    val resourceInfo = resourcesRegistrar.get(key)!!
    val now = updateUsageStatistics(key)
    val lock = ResourceLockImpl(now, resourceInfo, key, nextLockId++, this)
    key2Locks.getOrPut(key, { hashSetOf() }).add(lock)
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
    checkIfInterrupted()
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

    //Run the provision task in the current thread
    //if the thread has started the provision of this key first.
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
        val maxUnlockTime = lockTime.plus(expectedMaximumLockTime!!)
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
      val resourcesForEviction = evictionPolicy.selectResourcesForEviction(evictionInfo)

      if (resourcesForEviction.isNotEmpty()) {
        val disposedTotalWeight = resourcesForEviction.map { it.resourceInfo.weight }.reduce { acc, weight -> acc + weight }
        LOG.debug("$presentableName: it's time to evict unused resources. " +
            "Total weight: ${resourcesRegistrar.totalWeight}. " +
            "${resourcesForEviction.size} " + "resource".pluralize(resourcesForEviction.size) +
            " will be evicted with total weight $disposedTotalWeight"
        )
        for (availableFile in resourcesForEviction) {
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
    /**
     * Release the lock if the cleanup procedure has failed.
     */
    (result as? ResourceRepositoryResult.Found<*>)?.lockedResource?.closeOnException {
      cleanup()
    }
    return result
  }

  override fun toString() = presentableName

}