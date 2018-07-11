package com.jetbrains.pluginverifier.verifiers.resolution

import com.jetbrains.plugin.structure.classes.resolvers.InvalidClassFileException
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
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

  /**
   * Returns true if package with specified binary name exists.
   */
  fun packageExists(packageName: String): Boolean
}

/**
 * Resolves class [className] in [this] resolver and returns corresponding [ClsResolution].
 */
fun Resolver.resolveClassSafely(className: String): ClsResolution =
    try {
      findClass(className)
          ?.let { ClsResolution.Found(it) }
          ?: ClsResolution.NotFound
    } catch (e: InvalidClassFileException) {
      ClsResolution.InvalidClassFile(e.asmError)
    } catch (ie: InterruptedException) {
      throw ie
    } catch (e: Exception) {
      ClsResolution.FailedToReadClassFile(e.message ?: e.javaClass.name)
    }