package com.jetbrains.plugin.structure.jar

import java.io.Closeable
import java.io.Reader
import java.nio.file.Path

sealed class PluginDescriptorResult {
  data class Found(
    val path: Path,
    val reader: Reader
  ) : PluginDescriptorResult(), Closeable {
    override fun close() {
      reader.close()
    }
  }

  object NotFound : PluginDescriptorResult()

  data class Failed(val path: Path, val exception: Exception) : PluginDescriptorResult()
}

