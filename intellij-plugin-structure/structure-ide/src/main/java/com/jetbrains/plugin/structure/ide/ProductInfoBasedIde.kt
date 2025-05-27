/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide

import com.jetbrains.plugin.structure.intellij.platform.ProductInfo
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.jetbrains.annotations.ApiStatus
import java.nio.file.Path

class ProductInfoBasedIde private constructor(
  private val idePath: Path,
  private val version: IdeVersion,
  override val productInfo: ProductInfo,
  val pluginCollectionProvider: PluginCollectionProvider<Path>,
  private val pluginCollectionSource: ProductInfoPluginCollectionSource
) : Ide(), ProductInfoAware {

  private val _plugins = lazy {
    pluginCollectionProvider.getPlugins(pluginCollectionSource)
  }

  private val plugins by _plugins

  override fun getVersion() = version

  override fun getBundledPlugins(): List<IdePlugin> = plugins.toList()

  override fun hasBundledPlugin(pluginId: String): Boolean {
    return productInfo.layout.any { it.name == pluginId }
  }

  override fun getIdePath() = idePath

  @ApiStatus.Internal
  fun isPluginCollectionLoaded(): Boolean {
    return _plugins.isInitialized()
  }

  companion object {
    fun of(
      idePath: Path,
      version: IdeVersion,
      productInfo: ProductInfo,
      pluginCollectionProvider: PluginCollectionProvider<Path>
    ): ProductInfoBasedIde {
      val pluginCollectionSource = ProductInfoPluginCollectionSource(idePath, version, productInfo)
      return ProductInfoBasedIde(idePath, version, productInfo, pluginCollectionProvider, pluginCollectionSource)
    }
  }
}