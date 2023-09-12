/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.extractor

import com.jetbrains.plugin.structure.base.decompress.DecompressorSizeLimitExceededException
import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.base.plugin.Settings
import com.jetbrains.plugin.structure.base.problems.PluginFileSizeIsTooLarge
import com.jetbrains.plugin.structure.base.utils.*
import com.jetbrains.plugin.structure.intellij.problems.*
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path

object PluginExtractor {
  private val zipPathMatcher = FileSystems.getDefault().getPathMatcher("glob:*/lib/*.jar")

  fun extractPlugin(pluginFile: Path, extractDirectory: Path): ExtractorResult {
    Files.createDirectories(extractDirectory)
    val extractedPlugin = Files.createTempDirectory(extractDirectory, "plugin_")

    try {
      extractZip(pluginFile, extractedPlugin, Settings.INTELLIJ_PLUGIN_SIZE_LIMIT.getAsLong())
    } catch (e: DecompressorSizeLimitExceededException) {
      return fail(PluginFileSizeIsTooLarge(e.sizeLimit), extractedPlugin)
    } catch (e: Throwable) {
      extractedPlugin.deleteQuietly()
      throw e
    }

    return getExtractorResult(extractedPlugin)
  }

  private fun success(actualFile: Path, fileToDelete: Path): ExtractorResult =
    ExtractorResult.Success(ExtractedPlugin(actualFile, fileToDelete))

  private fun fail(problem: PluginProblem, extractedPlugin: Path): ExtractorResult {
    try {
      return ExtractorResult.Fail(problem)
    } finally {
      extractedPlugin.deleteQuietly()
    }
  }

  private fun getExtractorResult(extractedPlugin: Path): ExtractorResult {
    val rootFiles = extractedPlugin.listFiles()
    when (rootFiles.size) {
      0 -> return fail(PluginZipIsEmpty(), extractedPlugin)
      1 -> {
        val singleFile = rootFiles[0]
        return if (singleFile.isJar()) {
          fail(PluginZipContainsSingleJarInRoot(singleFile.simpleName), extractedPlugin)
        } else if (singleFile.isDirectory) {
          val allFiles = extractedPlugin.listAllFiles()
          if (allFiles.any { zipPathMatcher.matches(it) }) {
            success(singleFile, extractedPlugin)
          } else {
            fail(UnexpectedPluginZipStructure(), extractedPlugin)
          }
        } else {
          fail(PluginZipContainsUnknownFile(singleFile.simpleName), extractedPlugin)
        }
      }
      else -> return fail(PluginZipContainsMultipleFiles(rootFiles.map { it.simpleName }.sorted()), extractedPlugin)
    }
  }

}
