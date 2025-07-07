/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.base.problems.UnableToExtractZip
import com.jetbrains.plugin.structure.base.utils.simpleName
import com.jetbrains.plugin.structure.intellij.extractor.DefaultPluginExtractor
import com.jetbrains.plugin.structure.intellij.extractor.ExtractorResult
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.nio.file.Path

private val LOG: Logger = LoggerFactory.getLogger(PluginArchiveManager::class.java)

class PluginArchiveManager(private val extractDirectory: Path) {
  private val pluginExtractor = DefaultPluginExtractor()

  fun extractArchive(pluginFile: Path): Result {
    val extractorResult = try {
      pluginExtractor.extractPlugin(pluginFile, extractDirectory)
    } catch (e: Exception) {
      LOG.info("Unable to extract plugin zip ${pluginFile.simpleName}", e)
      return Result.Failed(pluginFile, UnableToExtractZip())
    }
    return when (extractorResult) {
      is ExtractorResult.Success -> {
        val extractedPlugin = extractorResult.extractedPlugin
        return Result.Extracted(pluginFile, extractedPlugin.pluginFile, extractedPlugin)
      }
      is ExtractorResult.Fail -> Result.Failed(pluginFile, extractorResult.pluginProblem)
    }
  }

  sealed class Result(open val artifactPath: Path) {
    data class Extracted(override val artifactPath: Path, val extractedPath: Path, val resourceToClose: Closeable) : Result(artifactPath)
    data class Failed(override val artifactPath: Path, val problem: PluginProblem) : Result(artifactPath)
  }
}