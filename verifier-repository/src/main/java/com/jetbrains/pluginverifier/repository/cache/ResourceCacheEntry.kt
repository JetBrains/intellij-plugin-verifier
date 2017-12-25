package com.jetbrains.pluginverifier.repository.cache

import com.jetbrains.pluginverifier.repository.resources.ResourceLock
import java.io.Closeable

/**
 * This class represents the [successful] [ResourceCacheEntryResult.Found]
 * result of [fetching] [ResourceCache.getResourceCacheEntry]
 * the resource from the [ResourceCache].
 *
 * It must be [closed] [close] after being used.
 * The [resource] must not be closed because
 * it will be closed by the [cache] [ResourceCache].
 */
data class ResourceCacheEntry<out R>(private val resourceLock: ResourceLock<R>) : Closeable {

  /**
   * The resource being [fetched] [ResourceCache.getResourceCacheEntry].
   * It will be deallocated when _this_ entry is [closed] [close].
   */
  val resource: R
    get() = resourceLock.resource

  override fun close() {
    resourceLock.close()
  }
}