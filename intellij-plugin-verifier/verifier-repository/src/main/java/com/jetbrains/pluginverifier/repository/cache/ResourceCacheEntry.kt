/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.repository.cache

import com.jetbrains.pluginverifier.repository.resources.ResourceInfo
import com.jetbrains.pluginverifier.repository.resources.ResourceLock
import com.jetbrains.pluginverifier.repository.resources.ResourceWeight
import java.io.Closeable

/**
 * This class represents the [successful][ResourceCacheEntryResult.Found]
 * result of [fetching][ResourceCache.getResourceCacheEntry]
 * the resource from the [ResourceCache].
 *
 * It must be [closed][close] after being used.
 * The [resource] must not be closed because
 * it will be closed by the [cache][ResourceCache].
 */
data class ResourceCacheEntry<out R, W : ResourceWeight<W>>(private val resourceLock: ResourceLock<R, W>) : Closeable {

  /**
   * The descriptor of the [fetched][ResourceCache.getResourceCacheEntry] resource.
   */
  val resourceInfo: ResourceInfo<R, W>
    get() = resourceLock.resourceInfo

  /**
   * The resource being [fetched][ResourceCache.getResourceCacheEntry].
   * It will be released when _this_ entry is [closed][close].
   */
  val resource: R
    get() = resourceInfo.resource

  /**
   * The [weight][ResourceWeight] of the [fetched][ResourceCache.getResourceCacheEntry] resource.
   */
  val resourceWeight: W
    get() = resourceInfo.weight

  override fun close() {
    resourceLock.close()
  }
}