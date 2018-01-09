package com.jetbrains.pluginverifier.parameters.jdk

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import java.io.Closeable
import java.nio.file.Path

/**
 * Holds class files  [resolver] [jdkClassesResolver] of the JDK
 * which is used to verify the plugins.
 */
data class JdkDescriptor(val jdkClassesResolver: Resolver,
                         val homeDir: Path) : Closeable {
  override fun toString(): String = homeDir.toAbsolutePath().toString()

  override fun close() = jdkClassesResolver.close()
}