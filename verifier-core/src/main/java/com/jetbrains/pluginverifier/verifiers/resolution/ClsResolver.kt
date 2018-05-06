package com.jetbrains.pluginverifier.verifiers.resolution

import java.io.Closeable

/**
 * Implementations of this interface provide
 * different strategies of classes resolution
 * depending on what verification is performed.
 *
 * They provide methods to [resolve] [resolveClass] classes
 * by names and determine their [origin] [getOriginOfClass] locations.
 */
interface ClsResolver : Closeable {
  /**
   * Attempts to resolve class by [className].
   */
  fun resolveClass(className: String): ClsResolution

  /**
   * Returns `true` if class by [className]
   * is external.
   */
  fun isExternalClass(className: String): Boolean

  /**
   * Returns `true` if class by [className]
   * can be resolved.
   */
  fun classExists(className: String): Boolean

  /**
   * Returns origin [location] [ClassFileOrigin] of class by [className].
   */
  fun getOriginOfClass(className: String): ClassFileOrigin?
}