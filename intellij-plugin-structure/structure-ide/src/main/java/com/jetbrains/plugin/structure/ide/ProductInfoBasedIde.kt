/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide

import com.jetbrains.plugin.structure.ide.layout.LayoutComponents
import com.jetbrains.plugin.structure.ide.layout.LayoutComponentsAware
import com.jetbrains.plugin.structure.intellij.platform.LayoutComponent
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
  private val pluginsByIdentifier = mutableMapOf<String, IdePlugin?>()
  private val scannedLayoutComponents = mutableSetOf<String>()
  private val lazyPluginLayoutComponentNames by lazy {
    layoutComponents.content
      .filterIsInstance<LayoutComponent.Plugin>()
      .map { it.name }
      .distinct()
  }

  private val plugins by _plugins

  override val layoutComponents: LayoutComponents
    get() = getPluginCollectionSource(LayoutComponents::class.java)?.resource
      ?: LayoutComponents.of(idePath, productInfo)

  override fun getVersion() = version

  override fun getBundledPlugins(): List<IdePlugin> = plugins

  override fun findPluginById(pluginId: String): IdePlugin? {
    if (_plugins.isInitialized()) {
      return super.findPluginById(pluginId)
    }

    findBundledPlugin(pluginId)?.let { return it }

    return if (hasAdditionalPluginCollectionProviders()) {
      super.findPluginById(pluginId)
    } else {
      null
    }
  }

  override fun findPluginByModule(moduleId: String): IdePlugin? {
    findBundledModule(moduleId)?.let { return it }
    return super.findPluginByModule(moduleId)
  }

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

  private fun findBundledModule(moduleId: String): IdePlugin? {
    if (_plugins.isInitialized()) {
      return null
    }

    val source = getPluginCollectionSource(LayoutComponents::class.java) as? ProductInfoLayoutComponentsPluginCollectionSource
      ?: return null
    val provider = pluginCollectionProviders[source] as? ProductInfoLayoutBasedPluginCollectionProvider
      ?: return null

    return provider.getModule(source, moduleId).also {
      cachePluginByIdentifier(it)
    }
  }

  private fun findBundledPlugin(pluginId: String): IdePlugin? {
    if (_plugins.isInitialized()) {
      return null
    }
    if (pluginsByIdentifier.containsKey(pluginId)) {
      return pluginsByIdentifier[pluginId]
    }

    val source = getPluginCollectionSource(LayoutComponents::class.java) as? ProductInfoLayoutComponentsPluginCollectionSource
      ?: return null
    val provider = pluginCollectionProviders[source] as? ProductInfoLayoutBasedPluginCollectionProvider
      ?: return null

    provider.getCorePlugin(source, pluginId)?.also {
      cachePluginByIdentifier(it)
      return it
    }

    provider.getPlugin(source, pluginId)?.takeIf {
      pluginIdentifier(it) == pluginId
    }?.also {
      cachePluginByIdentifier(it)
      return it
    }

    for (layoutComponentName in lazyPluginLayoutComponentNames) {
      if (!scannedLayoutComponents.add(layoutComponentName) || layoutComponentName == pluginId) {
        continue
      }

      provider.getPlugin(source, layoutComponentName)?.let {
        cachePluginByIdentifier(it)
      }

      if (pluginsByIdentifier.containsKey(pluginId)) {
        return pluginsByIdentifier[pluginId]
      }
    }

    pluginsByIdentifier[pluginId] = null
    return null
  }

  private fun cachePluginByIdentifier(plugin: IdePlugin?) {
    val identifier = pluginIdentifier(plugin) ?: return
    pluginsByIdentifier.putIfAbsent(identifier, plugin)
  }

  private fun pluginIdentifier(plugin: IdePlugin?): String? {
    plugin ?: return null
    return plugin.pluginId ?: plugin.pluginName
  }

  private fun hasAdditionalPluginCollectionProviders(): Boolean {
    return pluginCollectionProviders.keys.any { it !is ProductInfoLayoutComponentsPluginCollectionSource }
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