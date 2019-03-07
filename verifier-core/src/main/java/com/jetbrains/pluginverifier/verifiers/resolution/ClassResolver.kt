package com.jetbrains.pluginverifier.verifiers.resolution

import com.jetbrains.plugin.structure.base.utils.rethrowIfInterrupted
import com.jetbrains.plugin.structure.classes.resolvers.InvalidClassFileException
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import java.io.Closeable

/**
 * Implementations of this interface provide
 * different strategies of classes resolution
 * depending on what verification is performed.
 *
 * They provide methods to [resolve] [resolveClass] classes
 * by names and determine their origin locations.
 */
interface ClassResolver : Closeable {
  /**
   * Attempts to resolve class by [className].
   */
  fun resolveClass(className: String): ClassResolution

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
   * Returns origin location of class by [className].
   */
  fun getOriginOfClass(className: String): ClassFileOrigin?

  /**
   * Returns true if package with specified binary name exists.
   */
  fun packageExists(packageName: String): Boolean
}

/**
 * Resolves class [className] in [this] resolver and returns corresponding [ClassResolution].
 */
fun Resolver.resolveClassSafely(className: String): ClassResolution =
    try {
      findClass(className)
          ?.let { ClassResolution.Found(it) }
          ?: ClassResolution.NotFound
    } catch (e: InvalidClassFileException) {
      ClassResolution.InvalidClassFile(e.asmError)
    } catch (e: Exception) {
      e.rethrowIfInterrupted()
      ClassResolution.FailedToReadClassFile(e.message ?: e.javaClass.name)
    }