/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.extractor

import com.jetbrains.plugin.structure.base.decompress.DecompressorSizeLimitExceededException
import com.jetbrains.plugin.structure.base.plugin.Settings
import com.jetbrains.plugin.structure.base.problems.PluginFileSizeIsTooLarge
import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.base.utils.deleteQuietly
import com.jetbrains.plugin.structure.base.utils.extractZip
import com.jetbrains.plugin.structure.base.utils.isDirectory
import com.jetbrains.plugin.structure.base.utils.isJar
import com.jetbrains.plugin.structure.base.utils.listAllFiles
import com.jetbrains.plugin.structure.base.utils.listFiles
import com.jetbrains.plugin.structure.base.utils.simpleName
import com.jetbrains.plugin.structure.intellij.problems.PluginZipContainsMultipleFiles
import com.jetbrains.plugin.structure.intellij.problems.PluginZipContainsSingleJarInRoot
import com.jetbrains.plugin.structure.intellij.problems.PluginZipContainsUnknownFile
import com.jetbrains.plugin.structure.intellij.problems.PluginZipIsEmpty
import com.jetbrains.plugin.structure.intellij.problems.UnexpectedPluginZipStructure
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.PathMatcher

private const val JAR_IN_PLUGIN_DEPENDENCIES_GLOB = "glob:*/lib/*.jar"

private val LOG = LoggerFactory.getLogger(DefaultPluginExtractor::class.java)

class DefaultPluginExtractor : PluginExtractor {

  override fun extractPlugin(pluginFile: Path, extractDirectory: Path): ExtractorResult {
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
          if (allFiles.any { isJarInZip(it) }) {
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

  private fun isJarInZip(path: Path): Boolean {
    val pathFs = path.fileSystem
    return try {
      val pathMatcher: PathMatcher? = pathFs.getPathMatcher(JAR_IN_PLUGIN_DEPENDENCIES_GLOB)
      pathMatcher?.matches(path) ?: false
    } catch (e: Throwable) {
      LOG.warn("Cannot determine whether '$JAR_IN_PLUGIN_DEPENDENCIES_GLOB' is supported by the filesystem [{}]", pathFs.javaClass.name)
      false
    }
  }
}
