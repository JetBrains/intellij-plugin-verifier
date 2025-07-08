/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.base.problems.UnableToExtractZip
import com.jetbrains.plugin.structure.base.utils.Deletable
import com.jetbrains.plugin.structure.base.utils.exists
import com.jetbrains.plugin.structure.base.utils.simpleName
import com.jetbrains.plugin.structure.intellij.extractor.DefaultPluginExtractor
import com.jetbrains.plugin.structure.intellij.extractor.ExtractedPlugin
import com.jetbrains.plugin.structure.intellij.extractor.ExtractorResult
import com.jetbrains.plugin.structure.intellij.extractor.ExtractorResult.Fail
import com.jetbrains.plugin.structure.intellij.plugin.PluginArchiveManager.Result.Extracted
import com.jetbrains.plugin.structure.intellij.plugin.PluginArchiveManager.Result.Failed
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

private val LOG: Logger = LoggerFactory.getLogger(PluginArchiveManager::class.java)

class PluginArchiveManager(private val extractDirectory: Path) : Deletable, Closeable  {

  private val cache = ConcurrentHashMap<Path, Result>()

  private val pluginExtractor = DefaultPluginExtractor()

  @Synchronized
  fun extractArchive(path: Path): Result =
    cache.get(path)
      .takeIf { it is Extracted && it.resourceToClose.pluginFile.exists() }
      ?: doExtractArchive(path).also { it.cache() }

  fun doExtractArchive(pluginFile: Path): Result {
    val extractorResult = try {
      pluginExtractor.extractPlugin(pluginFile, extractDirectory)
    } catch (e: Exception) {
      LOG.info("Unable to extract plugin zip ${pluginFile.simpleName}", e)
      return Failed(pluginFile, UnableToExtractZip())
    }
    return when (extractorResult) {
      is ExtractorResult.Success -> {
        val extractedPlugin = extractorResult.extractedPlugin
        return Extracted(pluginFile, extractedPlugin.pluginFile, extractedPlugin)
      }
      is Fail -> Failed(pluginFile, extractorResult.pluginProblem)
    }
  }

  private fun Result.cache() {
    if (this is Extracted) {
      cache[this.artifactPath] = this
    }
  }

  @Synchronized
  fun clear() {
    cache.forEach { (_, result) ->
      if (result is Extracted) {
        result.resourceToClose.close()
      }
    }
    cache.clear()
  }

  override fun delete() {
    clear()
  }

  override fun close() {
    clear()
  }

  sealed class Result(open val artifactPath: Path) {
    data class Extracted(override val artifactPath: Path, val extractedPath: Path, val resourceToClose: ExtractedPlugin) : Result(artifactPath)
    data class Failed(override val artifactPath: Path, val problem: PluginProblem) : Result(artifactPath)
  }
}