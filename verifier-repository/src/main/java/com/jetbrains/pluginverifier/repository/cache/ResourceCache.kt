package com.jetbrains.pluginverifier.repository.cache

import com.jetbrains.pluginverifier.misc.closeOnException
import com.jetbrains.pluginverifier.repository.cleanup.SizeEvictionPolicy
import com.jetbrains.pluginverifier.repository.cleanup.SizeWeight
import com.jetbrains.pluginverifier.repository.provider.ResourceProvider
import com.jetbrains.pluginverifier.repository.resources.ResourceRepositoryImpl
import com.jetbrains.pluginverifier.repository.resources.ResourceRepositoryResult
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.time.Clock
import java.time.Duration
import java.time.Instant

/**
 * Resource cache is intended to cache any resources which
 * fetching may be expensive.
 *
 * The cache must be [closed] [close] on the application shutdown to
 * dispose the resources.
 */
class ResourceCache<R, in K>(
    /**
     * The maximum number of resources held by this cache at a moment.
     *
     * The [cleanup] [SizeEvictionPolicy] procedure will be
     * carried out once the cache size reaches this value.
     */
    cacheSize: Long,
    /**
     * The resource [provider] [ResourceProvider] that
     * provides the requested resources by [keys] [K].
     */
    resourceProvider: ResourceProvider<K, R>,
    /**
     * The disposer used to close the resources.
     *
     * The resources are closed either when the [cleanup] [SizeEvictionPolicy] procedure
     * determines to evict the corresponding resources,
     * or when the resources are removed from the [resourceRepository].
     * On [close], all the resources are removed and closed.
     */
    disposer: (R) -> Unit,
    /**
     * The cache name that can be used for logging and debug purposes
     */
    private val presentableName: String
) : Closeable {

  companion object {
    private val LOG: Logger = LoggerFactory.getLogger(ResourceCache::class.java)
  }

  /**
   * Resource [repository] [com.jetbrains.pluginverifier.repository.resources.ResourceRepository]
   * of the allocated [resources] [R].
   *
   * Initially, the repository is empty, meaning that there
   * are no resources opened.
   *
   * The repository is limited in size with [cacheSize]
   * parameter of the constructor.
   *
   * When the repository is full and a new resource is requested,
   * the unused resources are [closed] [disposer].
   */
  private val resourceRepository = ResourceRepositoryImpl(
      SizeEvictionPolicy(cacheSize),
      Clock.systemUTC(),
      resourceProvider,
      initialWeight = SizeWeight(0),
      weigher = { SizeWeight(1) },
      disposer = disposer,
      presentableName = presentableName,
      //it is fine for the cache entries to not be released for a long time.
      expectedMaximumLockTime = null
  )

  /**
   * A flag indicating whether _this_ cache is already closed.
   * It is protected by the synchronized blocks.
   */
  private var isClosed = false

  /**
   * Enqueues for closing all resources.
   * The resources that have no locks registered at this
   * moment will be closed immediately, while the locked resources
   * will be closed once they become released by their holders.
   *
   * The resources being requested at the time of [close] invocation will
   * be released and closed at the [getResourceCacheEntry].
   * Thus, no new keys can appear after the [close] is invoked.
   */
  override fun close() {
    LOG.info("Closing the $presentableName")
    synchronized(this) {
      if (!isClosed) {
        isClosed = true
        resourceRepository.removeAll()
      }
    }
  }

  /**
   * Provides the [ResourceCacheEntry] that contains
   * the [resource] [ResourceCacheEntry.resource].
   *
   * Possible results of this method invocation
   * are represented as instances of the [ResourceCacheEntryResult].
   * If the [Found] [ResourceCacheEntryResult.Found] is returned,
   * the corresponding [ResourceCacheEntry] must be
   * [closed] [ResourceCacheEntry.close] after being used.
   */
  fun getResourceCacheEntry(key: K): ResourceCacheEntryResult<R> {
    /**
     * Cancel the fetching if _this_ resource cache is already closed.
     */
    synchronized(this) {
      if (isClosed) {
        throw InterruptedException()
      }
    }
    val startTime = Instant.now()
    val repositoryResult = resourceRepository.get(key)
    val lockedResource = with(repositoryResult) {
      when (this) {
        is ResourceRepositoryResult.Found -> lockedResource
        is ResourceRepositoryResult.Failed -> return ResourceCacheEntryResult.Failed(reason, error)
        is ResourceRepositoryResult.NotFound -> return ResourceCacheEntryResult.NotFound(reason)
      }
    }
    /**
     * If _this_ cache was closed after the [key]
     * had been requested, release the lock and register
     * the [key] for deletion: it will be either
     * removed immediately, or after the last holder releases
     * its lock for the same [key].
     */
    synchronized(this) {
      if (isClosed) {
        lockedResource.release()
        resourceRepository.remove(key)
        throw InterruptedException()
      }
      return lockedResource.closeOnException {
        val fetchTime = Duration.between(startTime, Instant.now())
        ResourceCacheEntryResult.Found(ResourceCacheEntry(lockedResource), fetchTime)
      }
    }
  }

}