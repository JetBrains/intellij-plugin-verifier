package com.jetbrains.pluginverifier.repository.resources

import java.io.Closeable
import java.time.Instant

/**
 * Resource lock is used to indicate that the [resource]
 * stored in the [repository] [ResourceRepository] is being used at
 * the moment, and that it cannot be removed
 * until the lock is [released] [release] by the lock owner.
 */
interface ResourceLock<out R> : Closeable {
  /**
   * The point in the time when the resource was locked
   */
  val lockTime: Instant

  /**
   * The reference to the resource being locked
   */
  val resource: R

  /**
   * Releases the lock in the [repository] [ResourceRepository].
   * If there are no more locks for the [resource], the resource
   * can be safely removed.
   */
  fun release()

  /**
   * The close method gives the opportunity to use the try-with-resources expression.
   */
  override fun close() = release()
}