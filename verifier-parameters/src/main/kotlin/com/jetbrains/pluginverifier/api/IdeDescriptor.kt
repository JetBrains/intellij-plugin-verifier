package com.jetbrains.pluginverifier.api

import com.intellij.structure.ide.IdeVersion
import com.jetbrains.pluginverifier.ide.CreateIdeResult
import java.io.Closeable

sealed class IdeDescriptor(val ideVersion: IdeVersion) : Closeable {

  class ByInstance(val createIdeResult: CreateIdeResult) : IdeDescriptor(createIdeResult.ide.version) {
    override fun close() = createIdeResult.close()
  }

  override fun toString(): String = "$ideVersion"
}