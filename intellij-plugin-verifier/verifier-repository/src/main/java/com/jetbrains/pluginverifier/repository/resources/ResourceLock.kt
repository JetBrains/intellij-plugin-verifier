/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.repository.resources

import java.io.Closeable
import java.time.Instant

/**
 * Resource lock is used to indicate that a [resource]
 * stored in the [repository][ResourceRepository] is used at
 * the moment, thus it cannot be removed
 * until the lock is [released][release] by the lock owner.
 */
abstract class ResourceLock<out R, W : ResourceWeight<W>>(
  /**
   * The point in the time when the resource was locked
   */
  val lockTime: Instant,

  /**
   * The descriptor of the locked resource.
   */
  val resourceInfo: ResourceInfo<R, W>

) : Closeable {

  /**
   * The locked resource.
   */
  val resource: R
    get() = resourceInfo.resource

  /**
   * The [weight][ResourceWeight] of the locked resource.
   */
  val resourceWeight: W
    get() = resourceInfo.weight

  /**
   * Releases the lock in the [repository][ResourceRepository].
   *
   * If there are no more locks of the [resource], the resource
   * can be safely removed.
   */
  abstract fun release()

  /**
   * The close method allows using the try-with-resources expression.
   */
  final override fun close() = release()

}