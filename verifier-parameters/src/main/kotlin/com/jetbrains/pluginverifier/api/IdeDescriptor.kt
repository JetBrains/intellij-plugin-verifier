package com.jetbrains.pluginverifier.api

import com.intellij.structure.ide.IdeVersion
import com.jetbrains.pluginverifier.ide.CreateIdeResult
import java.io.Closeable

data class IdeDescriptor(val createIdeResult: CreateIdeResult) : Closeable {
  val ideVersion: IdeVersion = createIdeResult.ide.version

  override fun toString(): String = "$ideVersion"

  override fun close() = createIdeResult.close()
}