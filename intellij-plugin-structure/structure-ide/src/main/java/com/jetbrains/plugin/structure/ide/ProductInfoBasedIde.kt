/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide

import com.jetbrains.plugin.structure.ide.layout.LayoutComponents
import com.jetbrains.plugin.structure.ide.layout.LayoutComponentsAware
import com.jetbrains.plugin.structure.intellij.platform.ProductInfo
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.jetbrains.annotations.ApiStatus
import java.nio.file.Path

class ProductInfoBasedIde private constructor(
  private val idePath: Path,
  private val version: IdeVersion,
  override val productInfo: ProductInfo,
  private val pluginCollectionProviders: Map<PluginCollectionSource<Path, *>, PluginCollectionProvider<Path>>
) : Ide(), ProductInfoAware, LayoutComponentsAware {

  private val _plugins = lazy {
    pluginCollectionProviders.flatMap { (source, provider) ->
      provider.getPlugins(source)
    }
  }

  private val plugins by _plugins

  override val layoutComponents: LayoutComponents
    get() = getPluginCollectionSource(LayoutComponents::class.java)?.resource
      ?: LayoutComponents.of(idePath, productInfo)

  override fun getVersion() = version

  override fun getBundledPlugins(): List<IdePlugin> = plugins

  override fun hasBundledPlugin(pluginId: String): Boolean {
    return productInfo.layout.any { it.name == pluginId }
  }

  override fun getIdePath() = idePath

  @ApiStatus.Internal
  fun isPluginCollectionLoaded(): Boolean {
    return _plugins.isInitialized()
  }

  fun <T> getPluginCollectionSource(resourceType: Class<T>): PluginCollectionSource<Path, T>? {
    @Suppress("UNCHECKED_CAST")
    return pluginCollectionProviders.keys.find { resourceType.isInstance(it.resource) } as PluginCollectionSource<Path, T>?
  }

  companion object {
    fun of(
      idePath: Path,
      version: IdeVersion,
      productInfo: ProductInfo,
      pluginCollectionProviders: Map<PluginCollectionSource<Path, *>, PluginCollectionProvider<Path>>
    ): ProductInfoBasedIde {
      return ProductInfoBasedIde(idePath, version, productInfo, pluginCollectionProviders)
    }
  }
}