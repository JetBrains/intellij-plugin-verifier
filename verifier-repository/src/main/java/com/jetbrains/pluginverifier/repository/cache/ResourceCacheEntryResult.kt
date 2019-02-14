package com.jetbrains.pluginverifier.repository.cache

import java.time.Duration

/**
 * Represents possible results of fetching the resources from the [ResourceCache].
 */
sealed class ResourceCacheEntryResult<R> {
  /**
   * The resource cache entry has been successfully fetched.
   * The entry must be closed after the resource is used in order to
   * release the associated lock in the [ResourceCache].
   */
  data class Found<R>(
      val resourceCacheEntry: ResourceCacheEntry<R>,
      val fetchTime: Duration
  ) : ResourceCacheEntryResult<R>()

  /**
   * The resource cache entry was not fetched because the [error] had been thrown.
   */
  data class Failed<R>(val message: String, val error: Throwable) : ResourceCacheEntryResult<R>()

  /**
   * The resource cache entry was not fetched because it had not been found by the provider given to the [ResourceCache].
   */
  data class NotFound<R>(val message: String) : ResourceCacheEntryResult<R>()
}