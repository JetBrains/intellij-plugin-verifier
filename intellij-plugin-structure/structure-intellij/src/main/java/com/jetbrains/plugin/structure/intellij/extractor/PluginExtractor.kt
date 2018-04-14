package com.jetbrains.plugin.structure.intellij.extractor

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.base.utils.ZipUtil
import com.jetbrains.plugin.structure.base.utils.isZip
import com.jetbrains.plugin.structure.intellij.problems.PluginZipContainsMultipleFiles
import com.jetbrains.plugin.structure.intellij.problems.PluginZipContainsUnknownFile
import com.jetbrains.plugin.structure.intellij.problems.PluginZipIsEmpty
import org.apache.commons.io.FileUtils
import java.io.File
import java.nio.file.Files

object PluginExtractor {

  fun extractPlugin(pluginZip: File, extractDirectory: File): ExtractorResult {
    if (!pluginZip.isZip()) {
      throw IllegalArgumentException("Must be a zip archive: " + pluginZip)
    }

    val extractedPlugin = Files.createTempDirectory(extractDirectory.toPath(), "plugin_${pluginZip.nameWithoutExtension}_").toFile()

    try {
      ZipUtil.extractZip(pluginZip, extractedPlugin)
    } catch (e: Throwable) {
      FileUtils.deleteQuietly(extractedPlugin)
      throw e
    }

    return getExtractorResult(extractedPlugin)
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

  private fun getExtractorResult(extractedPlugin: File): ExtractorResult {
    val rootFiles = extractedPlugin.listFiles() ?: return fail(PluginZipIsEmpty(), extractedPlugin)
    if (rootFiles.isEmpty()) {
      return fail(PluginZipIsEmpty(), extractedPlugin)
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
        return fail(PluginZipContainsUnknownFile(singleFile.name), extractedPlugin)
      }
    } else {
      return fail(PluginZipContainsMultipleFiles(rootFiles.map { it.name }.sorted()), extractedPlugin)
    }
  }

}
