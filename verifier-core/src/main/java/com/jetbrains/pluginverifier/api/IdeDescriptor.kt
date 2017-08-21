package com.jetbrains.pluginverifier.api

import com.intellij.structure.ide.Ide
import com.intellij.structure.ide.IdeVersion
import com.intellij.structure.resolvers.Resolver
import java.io.Closeable

data class IdeDescriptor(val ide: Ide, val ideResolver: Resolver) : Closeable {
  val ideVersion: IdeVersion = ide.version

  override fun toString(): String = "$ideVersion"

  override fun close() = ideResolver.close()
}