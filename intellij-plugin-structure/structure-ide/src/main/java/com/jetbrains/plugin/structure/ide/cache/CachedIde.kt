/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide.cache

import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.ide.ProductInfoAware
import com.jetbrains.plugin.structure.ide.ProductInfoBasedIde
import com.jetbrains.plugin.structure.ide.layout.LayoutComponents
import com.jetbrains.plugin.structure.ide.layout.LayoutComponentsAware
import com.jetbrains.plugin.structure.intellij.platform.ProductInfo
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginImpl
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.slf4j.LoggerFactory
import java.nio.file.Path

private val LOG = LoggerFactory.getLogger(CachedIde::class.java)

/**
 * An [Ide] backed by a pre-populated plugin list loaded from the structural disk cache.
 * Returned on a cache hit; equivalent to [ProductInfoBasedIde] but with no lazy I/O.
 *
 * [validatedLayoutComponents] must be the same filtered view produced by
 * [ValidatingLayoutComponentsProvider][com.jetbrains.plugin.structure.ide.resolver.ValidatingLayoutComponentsProvider]
 * that was used during the original load, so that class resolvers built from [layoutComponents]
 * only reference JARs that actually exist on disk.
 */
internal class CachedIde(
  private val idePath: Path,
  private val ideVersion: IdeVersion,
  private val info: ProductInfo,
  private val cachedPlugins: List<IdePluginImpl>,
  private val validatedLayoutComponents: LayoutComponents
) : Ide(), ProductInfoAware, LayoutComponentsAware {

  override val productInfo: ProductInfo get() = info
  override fun getVersion(): IdeVersion = ideVersion
  override fun getIdePath(): Path = idePath
  override fun getBundledPlugins(): List<IdePlugin> = cachedPlugins
  override fun hasBundledPlugin(pluginId: String): Boolean = info.layout.any { it.name == pluginId }
  override val layoutComponents: LayoutComponents get() = validatedLayoutComponents
}

/**
 * Wraps a freshly created [ProductInfoBasedIde] and writes the result to the [cache] the first
 * time [getBundledPlugins] is called. Preserves the existing lazy-loading contract.
 */
internal class WriteThroughCachingIde(
  private val delegate: ProductInfoBasedIde,
  private val cache: IdeStructureCache,
  private val idePath: Path,
  private val info: ProductInfo,
  private val ideVersion: IdeVersion
) : Ide(), ProductInfoAware, LayoutComponentsAware {

  override val productInfo: ProductInfo get() = info
  override val layoutComponents: LayoutComponents get() = delegate.layoutComponents
  override fun getVersion(): IdeVersion = delegate.getVersion()
  override fun getIdePath(): Path = delegate.getIdePath()
  override fun hasBundledPlugin(pluginId: String): Boolean = delegate.hasBundledPlugin(pluginId)

  private val plugins by lazy {
    delegate.getBundledPlugins().also { plugins ->
      try {
        cache.put(idePath, info, ideVersion, plugins)
      } catch (e: Exception) {
        LOG.warn("Failed to populate IDE structure cache for {}: {}", ideVersion, e.message)
      }
    }
  }

  override fun getBundledPlugins(): List<IdePlugin> = plugins
}
