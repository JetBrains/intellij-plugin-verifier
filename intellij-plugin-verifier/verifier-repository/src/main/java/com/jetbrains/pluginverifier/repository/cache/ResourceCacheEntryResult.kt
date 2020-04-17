/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.repository.cache

import com.jetbrains.pluginverifier.repository.resources.ResourceWeight

/**
 * Represents possible results of fetching the resources from the [ResourceCache].
 */
sealed class ResourceCacheEntryResult<out R, W : ResourceWeight<W>> {
  /**
   * The resource cache entry has been successfully fetched.
   * The entry must be closed after the resource is used in order to
   * release the associated lock in the [ResourceCache].
   */
  data class Found<R, W : ResourceWeight<W>>(val resourceCacheEntry: ResourceCacheEntry<R, W>) : ResourceCacheEntryResult<R, W>()

  /**
   * The resource cache entry was not fetched because the [error] had been thrown.
   */
  data class Failed<R, W : ResourceWeight<W>>(val message: String, val error: Throwable) : ResourceCacheEntryResult<R, W>()

  /**
   * The resource cache entry was not fetched because it had not been found by the provider given to the [ResourceCache].
   */
  data class NotFound<R, W : ResourceWeight<W>>(val message: String) : ResourceCacheEntryResult<R, W>()
}