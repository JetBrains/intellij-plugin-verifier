package com.jetbrains.plugin.structure.intellij.extractor

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.base.utils.FileUtil
import com.jetbrains.plugin.structure.base.utils.ZipUtil
import com.jetbrains.plugin.structure.intellij.problems.PluginZipContainsMultipleFiles
import com.jetbrains.plugin.structure.intellij.problems.PluginZipContainsUnknownFile
import com.jetbrains.plugin.structure.intellij.problems.PluginZipIsEmpty
import org.apache.commons.io.FileUtils
import java.io.File

object PluginExtractor {

  fun extractPlugin(pluginZip: File, extractDirectory: File): ExtractorResult {
    if (!FileUtil.isZip(pluginZip)) {
      throw IllegalArgumentException("Must be a zip archive: " + pluginZip)
    }

    val extractedPlugin = FileUtil.createTempDir(extractDirectory, "plugin_${pluginZip.nameWithoutExtension}_")

    try {
      ZipUtil.extractZip(pluginZip, extractedPlugin)
    } catch (e: Throwable) {
      FileUtils.deleteQuietly(extractedPlugin)
      throw e
    }

    return getExtractorResult(pluginZip, extractedPlugin)
  }

  private fun success(actualFile: File, fileToDelete: File): ExtractorResult =
      ExtractorResult.Success(ExtractedPlugin(actualFile, fileToDelete))

  private fun fail(problem: PluginProblem, extractedPlugin: File): ExtractorResult {
    try {
      return ExtractorResult.Fail(problem)
    } finally {
      FileUtils.deleteQuietly(extractedPlugin)
    }
  }

  private fun getExtractorResult(pluginZip: File, extractedPlugin: File): ExtractorResult {
    val rootFiles = extractedPlugin.listFiles() ?: return fail(PluginZipIsEmpty(pluginZip), extractedPlugin)
    if (rootFiles.isEmpty()) {
      return fail(PluginZipIsEmpty(pluginZip), extractedPlugin)
    } else if (rootFiles.size == 1) {
      val singleFile = rootFiles[0]
      if (singleFile.name.endsWith(".jar")) {
        return success(singleFile, extractedPlugin)
      } else if (singleFile.isDirectory) {
        if (singleFile.name == "lib") {
          return success(extractedPlugin, extractedPlugin)
        } else {
          return success(singleFile, extractedPlugin)
        }
      } else {
        return fail(PluginZipContainsUnknownFile(pluginZip, singleFile.name), extractedPlugin)
      }
    } else {
      return fail(PluginZipContainsMultipleFiles(pluginZip, rootFiles.map { it.name }.sorted()), extractedPlugin)
    }
  }

}
