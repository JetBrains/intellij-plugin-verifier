package com.jetbrains.pluginverifier.api

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import java.io.Closeable

data class IdeDescriptor(val ide: Ide, val ideResolver: Resolver) : Closeable {
  val ideVersion: IdeVersion = ide.version

  override fun toString(): String = "$ideVersion"

  override fun close() = ideResolver.close()
}