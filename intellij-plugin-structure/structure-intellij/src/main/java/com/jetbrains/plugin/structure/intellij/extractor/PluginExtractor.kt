/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.extractor

import com.jetbrains.plugin.structure.base.decompress.DecompressorSizeLimitExceededException
import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.base.plugin.Settings
import com.jetbrains.plugin.structure.base.problems.PluginFileSizeIsTooLarge
import com.jetbrains.plugin.structure.base.utils.extractZip
import com.jetbrains.plugin.structure.intellij.problems.PluginZipContainsMultipleFiles
import com.jetbrains.plugin.structure.intellij.problems.PluginZipContainsUnknownFile
import com.jetbrains.plugin.structure.intellij.problems.PluginZipIsEmpty
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.InputStream
import java.nio.file.Files

object PluginExtractor {

  fun extractPlugin(pluginContent: InputStream, extractDirectory: File): ExtractorResult {
    Files.createDirectories(extractDirectory.toPath())
    val extractedPlugin = Files.createTempDirectory(extractDirectory.toPath(), "plugin_").toFile()

    try {
      extractZip(pluginContent, extractedPlugin, Settings.INTELLIJ_PLUGIN_SIZE_LIMIT.getAsLong())
    } catch (e: DecompressorSizeLimitExceededException) {
      return fail(PluginFileSizeIsTooLarge(e.sizeLimit), extractedPlugin)
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
    when {
      rootFiles.isEmpty() -> return fail(PluginZipIsEmpty(), extractedPlugin)
      rootFiles.size == 1 -> {
        val singleFile = rootFiles[0]
        return if (singleFile.name.endsWith(".jar")) {
          success(singleFile, extractedPlugin)
        } else if (singleFile.isDirectory) {
          if (singleFile.name == "lib") {
            success(extractedPlugin, extractedPlugin)
          } else {
            success(singleFile, extractedPlugin)
          }
        } else {
          fail(PluginZipContainsUnknownFile(singleFile.name), extractedPlugin)
        }
      }
      else -> return fail(PluginZipContainsMultipleFiles(rootFiles.map { it.name }.sorted()), extractedPlugin)
    }
  }

}
