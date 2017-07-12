package com.intellij.structure.impl.extractor

import com.jetbrains.structure.plugin.PluginProblem
import org.apache.commons.io.FileUtils
import java.io.Closeable
import java.io.File


data class ExtractedPluginFile(val actualPluginFile: File,
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
