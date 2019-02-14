package com.jetbrains.plugin.structure.intellij.extractor

import org.apache.commons.io.FileUtils
import java.io.Closeable
import java.io.File

data class ExtractedPlugin(
    val pluginFile: File,
    private val fileToDelete: File
) : Closeable {
  override fun close() {
    FileUtils.deleteQuietly(fileToDelete)
  }
}