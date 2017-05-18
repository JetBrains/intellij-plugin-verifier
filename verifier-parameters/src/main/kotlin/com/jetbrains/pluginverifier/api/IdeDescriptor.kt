package com.jetbrains.pluginverifier.api

import com.intellij.structure.ide.IdeVersion
import com.jetbrains.pluginverifier.ide.CreateIdeResult
import java.io.File

sealed class IdeDescriptor(val ideVersion: IdeVersion) {

  class ByFile(ideVersion: IdeVersion, val file: File) : IdeDescriptor(ideVersion)

  class ByInstance(val createIdeResult: CreateIdeResult) : IdeDescriptor(createIdeResult.ide.version)

  override fun toString(): String = "$ideVersion"
}