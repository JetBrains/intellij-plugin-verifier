package com.jetbrains.plugin.structure.intellij.extractor

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import org.apache.commons.io.FileUtils
import java.io.Closeable
import java.io.File


data class ExtractedPluginFile(val pluginFile: File,
                               val fileToDelete: File?) : Closeable {
  override fun close() {
    if (fileToDelete != null) {
      FileUtils.deleteQuietly(fileToDelete)
    }
  }
}

sealed class ExtractorResult

data class ExtractorSuccess(val extractedPlugin: ExtractedPluginFile) : ExtractorResult()

data class ExtractorFail(val pluginProblem: PluginProblem) : ExtractorResult()
