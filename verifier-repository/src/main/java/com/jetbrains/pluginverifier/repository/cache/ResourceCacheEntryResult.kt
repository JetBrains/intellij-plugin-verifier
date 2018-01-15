package com.jetbrains.pluginverifier.repository.cache

import java.time.Duration

/**
 * Represents possible results of [fetching] [ResourceCache.getResourceCacheEntry]
 * the resources from the [ResourceCache].
 */
sealed class ResourceCacheEntryResult<R> {
  /**
   * The resource cache [entry] [ResourceCacheEntry] has been
   * successfully [fetched] [ResourceCache.getResourceCacheEntry].
   * The [entry] [resourceCacheEntry] must be closed after
   * the resource's usage to release the associated lock in the [ResourceCache].
   */
  data class Found<R>(val resourceCacheEntry: ResourceCacheEntry<R>,
                      val fetchTime: Duration) : ResourceCacheEntryResult<R>()

  /**
   * The resource cache [entry] [ResourceCacheEntry] has not
   * been [fetched] [ResourceCache.getResourceCacheEntry]
   * because the [error] had been thrown.
   */
  data class Failed<R>(val message: String, val error: Throwable) : ResourceCacheEntryResult<R>()

  /**
   * The resource cache [entry] [ResourceCacheEntry] has not
   * been [fetched] [ResourceCache.getResourceCacheEntry]
   * because it had not been found by the [provider] [com.jetbrains.pluginverifier.repository.provider.ResourceProvider]
   * given to the [ResourceCache].
   */
  data class NotFound<R>(val message: String) : ResourceCacheEntryResult<R>()
}