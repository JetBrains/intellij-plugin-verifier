/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide.cache

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.jetbrains.plugin.structure.intellij.platform.ProductInfo
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginImpl
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.BasicFileAttributes

private val LOG = LoggerFactory.getLogger(IdeStructureCache::class.java)

/**
 * Caches the structural representation of an IDE (bundled plugins with all metadata) to disk,
 * keyed by IDE version and the modification time of `product-info.json`.
 */
interface IdeStructureCache {
  /**
   * Returns the cached list of plugins for the given IDE, or `null` on a cache miss or stale entry.
   */
  fun get(idePath: Path, ideVersion: IdeVersion): List<IdePluginImpl>?

  /**
   * Writes the plugin list for the given IDE to the cache.
   */
  fun put(idePath: Path, productInfo: ProductInfo, ideVersion: IdeVersion, plugins: List<IdePlugin>)
}

class DiskIdeStructureCache(private val cacheDir: Path) : IdeStructureCache {

  private val mapper = ObjectMapper().registerModule(kotlinModule())

  companion object {
    /** Resolves the default cache dir from the standard plugin-verifier home directory. */
    fun default(): DiskIdeStructureCache {
      val home = System.getProperty("plugin.verifier.home.dir")
        ?.let { Path.of(it) }
        ?: Path.of(System.getProperty("user.home"), ".pluginVerifier")
      return DiskIdeStructureCache(home.resolve("ideCache"))
    }
  }

  override fun get(idePath: Path, ideVersion: IdeVersion): List<IdePluginImpl>? {
    val cacheFile = cacheFileFor(ideVersion)
    if (!Files.exists(cacheFile)) return null

    return try {
      val descriptor: CachedIdeDescriptor = mapper.readValue(cacheFile.toFile())
      if (descriptor.cacheVersion != CACHE_FORMAT_VERSION) {
        Files.deleteIfExists(cacheFile)
        return null
      }
      val mtime = productInfoMtime(idePath)
      if (descriptor.productInfoMtime != mtime) return null

      val builder = newSaxBuilder()
      descriptor.plugins.map { it.toIdePluginImpl(idePath, builder) }
    } catch (e: Exception) {
      LOG.warn("Discarding unreadable IDE structure cache at {}: {}", cacheFile, e.message)
      Files.deleteIfExists(cacheFile)
      null
    }
  }

  override fun put(idePath: Path, productInfo: ProductInfo, ideVersion: IdeVersion, plugins: List<IdePlugin>) {
    try {
      Files.createDirectories(cacheDir)
      val outputter = newXmlOutputter()
      val descriptor = CachedIdeDescriptor(
        ideVersion = ideVersion.asString(),
        productInfoMtime = productInfoMtime(idePath),
        plugins = plugins.map { it.toCachedPluginData(idePath, outputter) }
      )
      val cacheFile = cacheFileFor(ideVersion)
      val tmp = cacheFile.parent.resolve(cacheFile.fileName.toString() + ".tmp")
      mapper.writeValue(tmp.toFile(), descriptor)
      Files.move(tmp, cacheFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
      LOG.debug("Wrote IDE structure cache to {}", cacheFile)
    } catch (e: Exception) {
      LOG.warn("Failed to write IDE structure cache for {}: {}", ideVersion, e.message)
    }
  }

  private fun cacheFileFor(ideVersion: IdeVersion): Path {
    val safeName = ideVersion.asString().replace(Regex("[^a-zA-Z0-9._-]"), "_")
    return cacheDir.resolve("$safeName.json")
  }

  private fun productInfoMtime(idePath: Path): Long {
    return sequenceOf(
      idePath.resolve("product-info.json"),
      idePath.resolve("Resources/product-info.json")
    ).firstOrNull { Files.exists(it) }?.let {
      Files.readAttributes(it, BasicFileAttributes::class.java).lastModifiedTime().toMillis()
    } ?: 0L
  }
}
