package com.jetbrains.pluginverifier.repository.resources

import com.jetbrains.pluginverifier.misc.checkIfInterrupted
import com.jetbrains.pluginverifier.misc.closeOnException
import com.jetbrains.pluginverifier.misc.pluralize
import com.jetbrains.pluginverifier.repository.cleanup.UsageStatistic
import com.jetbrains.pluginverifier.repository.provider.ProvideResult
import com.jetbrains.pluginverifier.repository.provider.ResourceProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.Instant
import java.util.concurrent.FutureTask

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
                                   private val presentableName: String = "ResourceRepository") : ResourceRepository<R, K> {

  private val logger: Logger = LoggerFactory.getLogger(presentableName)

  private val resourcesRegistrar = RepositoryResourcesRegistrar<R, K>(initialWeight, weigher, disposer, logger)

  private var nextLockId: Long = 0

  private val key2Locks = hashMapOf<K, MutableSet<ResourceLock<R>>>()

  private val removeQueue = hashSetOf<K>()

  private val waitedKeys = hashSetOf<K>()

  private val additionTasks = hashMapOf<K, FutureTask<ProvideResult<R>>>()

  private val statistics = hashMapOf<K, UsageStatistic>()

  /**
   * todo: addition can exceed limit of the [evictionPolicy]
   * add the resource only if its weight is less than the limit
   * otherwise wait for the available weight to appear.
   */
  @Synchronized
  override fun add(key: K, resource: R): Boolean {
    if (resourcesRegistrar.addResource(key, resource)) {
      assert(key !in statistics)
      updateUsageStatistics(key)
      cleanup()
      return true
    }
    return false
  }

  @Synchronized
  override fun <R> lockAndExecute(block: () -> R): R = block()

  @Synchronized
  override fun getAllExistingKeys() = resourcesRegistrar.getAllKeys().toSet()

  @Synchronized
  override fun has(key: K) = resourcesRegistrar.has(key)

  @Synchronized
  override fun remove(key: K): Boolean = if (isLockedKey(key) || isBeingProvided(key)) {
    logger.debug("remove($key): the resource is locked or is being provided, enqueue for removing later.")
    removeQueue.add(key)
    false
  } else if (resourcesRegistrar.has(key)) {
    logger.debug("remove($key): the resource is not locked, deleting now")
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
  private fun registerLock(key: K): ResourceLock<R> {
    assert(resourcesRegistrar.has(key))
    val resourceInfo = resourcesRegistrar.get(key)!!
    val now = updateUsageStatistics(key)
    val lockId = nextLockId++
    val lock = ResourceLockImpl(now, resourceInfo, key, lockId, this)
    logger.debug("get($key): lock is registered $lock ")
    key2Locks.getOrPut(key, { hashSetOf() }).add(lock)
    return lock
  }

  @Synchronized
  internal fun releaseLock(lock: ResourceLockImpl<R, K>) {
    val key = lock.key
    val resourceLocks = key2Locks[key]
    if (resourceLocks != null) {
      logger.debug("releasing lock $lock")
      resourceLocks.remove(lock)
      if (resourceLocks.isEmpty()) {
        key2Locks.remove(key)

        if (key in removeQueue) {
          logger.debug("removing the $key as it is enqueued for removing and it has been just released")
          removeQueue.remove(key)
          doRemove(key)
        }
      }
    } else {
      logger.debug("attempt to release an unregistered lock $lock")
    }
  }

  @Synchronized
  private fun doRemove(key: K) {
    assert(key !in additionTasks)
    assert(key !in waitedKeys)
    resourcesRegistrar.removeResource(key)
    statistics.remove(key)
  }

  private fun getOrWait(key: K): ResourceRepositoryResult<R> {
    checkIfInterrupted()
    val (addTask, runInCurrentThread) = synchronized(this) {
      if (resourcesRegistrar.has(key)) {
        val lock = registerLock(key)
        logger.debug("get($key): the resource is available and a lock is registered $lock")
        return ResourceRepositoryResult.Found(lock)
      }

      waitedKeys.add(key)
      val task = additionTasks[key]
      if (task != null) {
        logger.debug("get($key): waiting for another thread to finish fetching the resource")
        task to false
      } else {
        logger.debug("get($key): fetching the resource in the current thread")
        val addTask = FutureTask {
          getAndAddResource(key)
        }
        additionTasks[key] = addTask
        addTask to true
      }
    }

    //Run the provision task in the current thread
    //if the thread has started the provision of this key first.
    if (runInCurrentThread) {
      addTask.run()
    }

    try {
      val provideResult = addTask.get()
      return provideResult.registerLockIfProvided(key)
    } finally {
      synchronized(this) {
        waitedKeys.remove(key)

        if (runInCurrentThread) {
          additionTasks.remove(key)
        }
      }
    }
  }

  private fun getAndAddResource(key: K): ProvideResult<R> {
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
  override fun cleanup() {
    if (evictionPolicy.isNecessary(resourcesRegistrar.totalWeight)) {
      val availableResources = resourcesRegistrar.resources.map { (key, resourceInfo) ->
        AvailableResource(key, resourceInfo, statistics[key]!!, isLockedKey(key))
      }

      val evictionInfo = EvictionInfo(resourcesRegistrar.totalWeight, availableResources)
      val resourcesForEviction = evictionPolicy.selectResourcesForEviction(evictionInfo)

      if (resourcesForEviction.isNotEmpty()) {
        val disposedTotalWeight = resourcesForEviction.map { it.resourceInfo.weight }.reduce { acc, weight -> acc + weight }
        logger.debug("it's time to evict unused resources. " +
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
    val result = getOrWait(key)
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