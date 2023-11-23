package com.jetbrains.plugin.structure.jar

import java.io.Closeable
import java.io.InputStream
import java.nio.file.Path

sealed class PluginDescriptorResult {
  data class Found(
    val path: Path,
    val inputStream: InputStream
  ) : PluginDescriptorResult(), Closeable {
    override fun close() {
      inputStream.close()
    }
  }

  object NotFound : PluginDescriptorResult()

  data class Failed(val path: Path, val exception: Exception) : PluginDescriptorResult()
}

