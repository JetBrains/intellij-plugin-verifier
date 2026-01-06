/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.repository.resources

/**
 * Possible results of [provision][ResourceRepository.get] of the resource.
 */
sealed class ResourceRepositoryResult<out R, W : ResourceWeight<W>> {
  /**
   * The resource is provided and the [resource lock][lockedResource]
   * is registered for it.
   */
  data class Found<out R, W : ResourceWeight<W>>(val lockedResource: ResourceLock<R, W>) : ResourceRepositoryResult<R, W>()

  /**
   * The resource is not found due to [reason].
   */
  data class NotFound<out R, W : ResourceWeight<W>>(val reason: String) : ResourceRepositoryResult<R, W>()

  /**
   * The resource is failed to be provided due to [reason].
   * The exception thrown on provision is [error].
   */
  data class Failed<out R, W : ResourceWeight<W>>(val reason: String, val error: Exception) : ResourceRepositoryResult<R, W>()
}