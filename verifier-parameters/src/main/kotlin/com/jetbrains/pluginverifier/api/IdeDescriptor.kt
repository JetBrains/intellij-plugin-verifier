package com.jetbrains.pluginverifier.api

import com.intellij.structure.domain.Ide
import com.intellij.structure.domain.IdeVersion
import com.intellij.structure.resolvers.Resolver
import java.io.File

sealed class IdeDescriptor(val ideVersion: IdeVersion) {

  class ByFile(ideVersion: IdeVersion, val file: File) : IdeDescriptor(ideVersion)

  class ByInstance(val ide: Ide, val ideResolver: Resolver) : IdeDescriptor(ide.version)

  override fun toString(): String = "$ideVersion"
}