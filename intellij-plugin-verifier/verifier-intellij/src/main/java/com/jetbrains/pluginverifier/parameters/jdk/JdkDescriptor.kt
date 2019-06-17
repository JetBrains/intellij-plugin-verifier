package com.jetbrains.pluginverifier.parameters.jdk

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import java.io.Closeable
import java.nio.file.Path

/**
 * Holder of class files of the JDK.
 */
data class JdkDescriptor(val jdkPath: Path, val jdkResolver: Resolver) : Closeable {
  override fun toString(): String = jdkPath.toAbsolutePath().toString()

  override fun close() = jdkResolver.close()
}