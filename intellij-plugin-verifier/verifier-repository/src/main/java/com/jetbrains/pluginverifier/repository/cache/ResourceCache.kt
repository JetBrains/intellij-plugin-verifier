/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.repository.cache

import com.jetbrains.plugin.structure.base.utils.closeOnException
import com.jetbrains.pluginverifier.repository.cleanup.SizeEvictionPolicy
import com.jetbrains.pluginverifier.repository.cleanup.SizeWeight
import com.jetbrains.pluginverifier.repository.provider.ResourceProvider
import com.jetbrains.pluginverifier.repository.resources.EvictionPolicy
import com.jetbrains.pluginverifier.repository.resources.ResourceRepositoryImpl
import com.jetbrains.pluginverifier.repository.resources.ResourceRepositoryResult
import com.jetbrains.pluginverifier.repository.resources.ResourceWeight
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.time.Clock

/**
 * Resource cache is intended to cache any resources which
 * fetching may be expensive.
 *
 * Initially, the cache is empty.
 * The resources are fetched and cached on demand: if a resource
 * requested by a [key][K] is not available in the cache,
 * the [resourceProvider] provides the corresponding resource. It works
 * concurrently, meaning that in case several threads request a resource
 * by the same key, only one of them will actually provide the resource,
 * while others will wait for the first to complete.
 *
 * The resources are [returned][getResourceCacheEntry] wrapped in [ResourceCacheEntry]
 * that protect them from eviction from the cache while the resources
 * are used by requesting threads. Only once all the [cache entries][ResourceCacheEntry]
 * of a resource by a specific [key][K] get [closed][ResourceCacheEntry.close],
 * the resource _may be_ [disposed][disposer]. Note that it may not happen immediately
 * since the same resource may be requested once again shortly.
 *
 * While there are available "slots" in the cache, the resources are not disposed.
 * All the unreleased resources will be [disposed][disposer] once the cache is [closed][close].
 */
class ResourceCache<R, in K, W : ResourceWeight<W>>(
  /**
   * [ResourceProvider] that provides the
   * requested resources by [keys][K].
   */
  resourceProvider: ResourceProvider<K, R>,
  /**
   * The disposer used to close the resources.
   *
   * The resources are closed either when the [cleanup][SizeEvictionPolicy] procedure
   * determines to evict the corresponding resources,
   * or when the resources are removed from the [resourceRepository].
   * On [close], all the resources are removed and closed.
   */
  disposer: (R) -> Unit,
  /**
   * [EvictionPolicy] that manages eviction of resources
   * held in this cache.
   */
  evictionPolicy: EvictionPolicy<R, K, W>,
  /**
   * Initial weight of the resources held in this cache.
   */
  initialWeight: W,
  /**
   * Weigher of the resources held in this cache.
   */
  weigher: (R) -> W,
  /**
   * The cache name that can be used for logging and debugging purposes
   */
  private val presentableName: String
) : Closeable {

  companion object {
    private val LOG: Logger = LoggerFactory.getLogger(ResourceCache::class.java)
  }

  /**
   * Resource [repository][com.jetbrains.pluginverifier.repository.resources.ResourceRepository]
   * of the allocated [resources][R].
   *
   * Initially, the repository is empty, meaning that there
   * are no resources opened.
   *
   * When the repository is full and a new resource is requested,
   * unused resources are [disposed][disposer].
   */
  private val resourceRepository = ResourceRepositoryImpl(
    evictionPolicy,
    Clock.systemUTC(),
    resourceProvider,
    initialWeight,
    weigher,
    disposer,
    presentableName
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
   * Thus, no new resources can be allocated after the [close] is invoked.
   */
  @Synchronized
  override fun close() {
    LOG.debug("Closing the $presentableName")
    if (!isClosed) {
      isClosed = true
      resourceRepository.removeAll()
    }
  }

  /**
   * Provides the [ResourceCacheEntry] that contains
   * the [resource][ResourceCacheEntry.resource].
   *
   * Possible results of this method invocation
   * are represented as instances of the [ResourceCacheEntryResult].
   * If the [Found][ResourceCacheEntryResult.Found] is returned,
   * the corresponding [ResourceCacheEntry] must be
   * [closed][ResourceCacheEntry.close] after being used.
   */
  @Throws(InterruptedException::class)
  fun getResourceCacheEntry(key: K): ResourceCacheEntryResult<R, W> {
    /**
     * Cancel the fetching if _this_ resource cache is already closed.
     */
    synchronized(this) {
      if (isClosed) {
        throw InterruptedException()
      }
    }
    val repositoryResult = resourceRepository.get(key)
    val lockedResource = with(repositoryResult) {
      when (this) {
        is ResourceRepositoryResult.Found<R, W> -> lockedResource
        is ResourceRepositoryResult.Failed<*, *> -> return ResourceCacheEntryResult.Failed(reason, error)
        is ResourceRepositoryResult.NotFound<*, *> -> return ResourceCacheEntryResult.NotFound(reason)
      }
    }
    /**
     * If _this_ cache was closed after the [key]
     * had been requested, release the lock and register
     * the [key] for deletion: it will be either
     * removed immediately, or just after the last
     * holder releases the lock.
     */
    synchronized(this) {
      if (isClosed) {
        lockedResource.release()
        resourceRepository.remove(key)
        throw InterruptedException()
      }
      return lockedResource.closeOnException {
        ResourceCacheEntryResult.Found(ResourceCacheEntry(lockedResource))
      }
    }
  }

  /**
   * Explicitly remove the resource from this cache and invoke the associated disposer.
   */
  fun remove(key: K) {
    resourceRepository.remove(key)
  }

}

fun <K, R> createSizeLimitedResourceCache(
  cacheSize: Int,
  resourceProvider: ResourceProvider<K, R>,
  disposer: (R) -> Unit,
  presentableName: String
): ResourceCache<R, K, SizeWeight> = ResourceCache(
  resourceProvider,
  disposer,
  SizeEvictionPolicy(cacheSize),
  SizeWeight(0),
  { SizeWeight(1) },
  presentableName
)