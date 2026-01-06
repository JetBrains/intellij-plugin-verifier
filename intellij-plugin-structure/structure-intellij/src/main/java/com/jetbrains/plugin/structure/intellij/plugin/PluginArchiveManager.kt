/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.base.decompress.DecompressorException
import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.base.problems.UnableToExtractZip
import com.jetbrains.plugin.structure.base.utils.Deletable
import com.jetbrains.plugin.structure.base.utils.Striped
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
import kotlin.system.measureTimeMillis

private val LOG: Logger = LoggerFactory.getLogger(PluginArchiveManager::class.java)

class PluginArchiveManager(private val extractDirectory: Path, private val isCollectingStats: Boolean = true) : Deletable, Closeable  {

  private val cache = ConcurrentHashMap<Path, Result>()

  private val pluginExtractor = DefaultPluginExtractor()

  val stats: Stats? = if (isCollectingStats) Stats() else null

  val locks = Striped.lock(Runtime.getRuntime().availableProcessors().coerceIn(4..32))

  fun extractArchive(path: Path): Result {
    val cached = getCached(path)
    if (cached != null) return cached
    synchronized(locks.get(path.toAbsolutePath().toString())) {
      val cached = getCached(path)
      if (cached != null) return cached
      return doExtractArchive(path)
    }
  }

  private fun getCached(path: Path): Result? {
    val cached = cache[path]
      .takeIf { it is Extracted && it.resourceToClose.pluginFile.exists() }
      ?.also { it.logCached() }
    return cached
  }

  private fun doExtractArchive(pluginFile: Path): Result {
    lateinit var extractorResult: ExtractorResult
    val extractionDuration = measureTimeMillis {
      extractorResult = runCatching {
        pluginExtractor.extractPlugin(pluginFile, extractDirectory)
      }.getOrElse {
        LOG.info("Unable to extract plugin zip ${pluginFile.simpleName}", it)
        if (it is DecompressorException) {
          Fail(UnableToExtractZip(it.message))
        } else {
          Fail(UnableToExtractZip())
        }
      }
    }
    return when (val extraction = extractorResult) {
      is ExtractorResult.Success -> {
        val extractedPlugin = extraction.extractedPlugin
        return Extracted(pluginFile, extractedPlugin.pluginFile, extractedPlugin).also {
          it.cache(extractionDuration)
        }
      }
      is Fail -> Failed(pluginFile, extraction.pluginProblem)
    }
  }

  private fun Extracted.cache(extractionDuration: Long) {
    cache[this.artifactPath] = this
    logCreated(extractionDuration)
  }

  private fun Result.logCached() {
    stats?.run { logCached(artifactPath) }
  }

  private fun Result.logCreated(extractionDuration: Long) {
    stats?.run { logCreated(artifactPath, extractionDuration) }
  }

  fun clear() {
    do {
      var removed = 0
      val iterator = cache.entries.iterator()
      while (iterator.hasNext()) {
        val entry = iterator.next()
        val result = entry.value
        if (result is Extracted) {
          result.resourceToClose.close()
        }
        iterator.remove()
        removed++
      }
    } while (removed > 0)
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

  class Stats {
    class Event(val path: Path, var createdCount: Int = 0, var cacheHit: Int = 0, val duration: Long) {
      override fun toString(): String {
        return "$path hit cache: $cacheHit, created: $createdCount in $duration ms"
      }
    }

    val events = ConcurrentHashMap<Path, Event>()

    fun logCreated(path: Path, extractionDuration: Long) {
      events.computeIfAbsent(path) { Event(path, duration = extractionDuration) }.createdCount++
    }

    fun logCached(path: Path) {
      events.computeIfAbsent(path) { Event(path, duration = 0) }.cacheHit++
    }
  }
}