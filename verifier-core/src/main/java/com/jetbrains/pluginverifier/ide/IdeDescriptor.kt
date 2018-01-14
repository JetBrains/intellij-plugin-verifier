package com.jetbrains.pluginverifier.ide

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import java.io.Closeable

/**
 * Holder of the [IDE] [ide] instance and its class files [resolver] [ideResolver].
 */
data class IdeDescriptor(val ide: Ide, val ideResolver: Resolver) : Closeable {
  val ideVersion: IdeVersion = ide.version

  override fun toString() = ideVersion.toString()

  override fun close() = ideResolver.close()
}