/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide

import com.jetbrains.plugin.structure.ide.layout.LayoutComponents
import com.jetbrains.plugin.structure.ide.layout.LayoutComponentsAware
import com.jetbrains.plugin.structure.intellij.platform.BundledModulesManager
import com.jetbrains.plugin.structure.intellij.platform.BundledModulesResolver
import com.jetbrains.plugin.structure.intellij.platform.LayoutComponent
import com.jetbrains.plugin.structure.intellij.platform.ProductInfo
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.PluginProviderResult
import com.jetbrains.plugin.structure.intellij.plugin.dependencies.id
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.plugin.structure.jar.SingletonCachingJarFileSystemProvider
import org.slf4j.LoggerFactory
import java.nio.file.Path

private val LOG = LoggerFactory.getLogger(InflatableProductInfoBasedIde::class.java)

class InflatableProductInfoBasedIde private constructor(
  private val idePath: Path,
  private val version: IdeVersion,
  override val productInfo: ProductInfo,
  private val pluginCollectionProviders: Map<PluginCollectionSource<Path, *>, PluginCollectionProvider<Path>>
) : Ide(), ProductInfoAware, LayoutComponentsAware {

  override val layoutComponents: LayoutComponents
    get() = getPluginCollectionSource(LayoutComponents::class.java)?.resource
      ?: LayoutComponents.of(idePath, productInfo)

  override fun getVersion() = version

  override fun getBundledPlugins(): List<IdePlugin> {
    LOG.debug("Accessing all bundled plugins")
    return pluginCache.values.toList()
  }

  private val moduleManager = BundledModulesManager(BundledModulesResolver(idePath, SingletonCachingJarFileSystemProvider))

  private val pluginCache = mutableMapOf<String, IdePlugin>()

  init {
    val (source, provider) = pluginCollectionProviders.entries.first { it.key is ProductInfoLayoutComponentsPluginCollectionSource } as Map.Entry<ProductInfoLayoutComponentsPluginCollectionSource, ProductInfoLayoutBasedPluginCollectionProvider>
    provider.getCorePlugins(source).forEach { plugin -> pluginCache[plugin.id] = plugin }
  }

  override fun findPluginById(pluginId: String): IdePlugin? {
    pluginCache[pluginId]?.let { return it }

    val (source, provider) = pluginCollectionProviders.entries.first { it.key is ProductInfoLayoutComponentsPluginCollectionSource } as Map.Entry<ProductInfoLayoutComponentsPluginCollectionSource, ProductInfoLayoutBasedPluginCollectionProvider>
    val layout = source.layoutComponents.layoutComponents.find { it.name == pluginId }

    if (layout == null) {
      LOG.debug("Could not find plugin with id: {}", pluginId)
      return null
    }

    return provider.getPlugin(moduleManager, source, layout.layoutComponent)?.also { pluginCache[pluginId] = it }
  }

  override fun findPluginByModule(moduleId: String): IdePlugin? {
    pluginCache[moduleId]?.let { return it }

    val (source, provider) = pluginCollectionProviders.entries.first { it.key is ProductInfoLayoutComponentsPluginCollectionSource } as Map.Entry<ProductInfoLayoutComponentsPluginCollectionSource, ProductInfoLayoutBasedPluginCollectionProvider>
    val layout = source.layoutComponents.layoutComponents.find { it.name == moduleId }

    if (layout != null && layout.layoutComponent !is LayoutComponent.PluginAlias) {
      return provider.getPlugin(moduleManager, source, layout.layoutComponent)?.also { pluginCache[moduleId] = it }
    }

    // PluginAlias has no back-reference — scan Plugin entries for the one declaring this moduleId
    val owningPlugin = source.layoutComponents.layoutComponents
      .filter { it.layoutComponent is LayoutComponent.Plugin }
      .mapNotNull { entry ->
        pluginCache[entry.name]
          ?: provider.getPlugin(moduleManager, source, entry.layoutComponent)?.also { pluginCache[entry.name] = it }
      }
      .firstOrNull { it.hasDefinedModuleWithId(moduleId) }

    if (owningPlugin != null) {
      return owningPlugin.also { pluginCache[moduleId] = it }
    }

    // Fall back to bundled module descriptors (module-descriptors.jar)
    val module = moduleManager.findModuleByName(moduleId)
    if (module != null) {
      val layoutComponent = LayoutComponent.ModuleV2(
        name = module.name,
        classPaths = module.resources.map { it.path.toString() }
      )
      return provider.getPlugin(moduleManager, source, layoutComponent)?.also { pluginCache[moduleId] = it }
    }

    return null
  }

  override fun findPluginByIdOrModuleId(pluginIdOrModuleId: String): PluginProviderResult? {
    findPluginById(pluginIdOrModuleId)?.let {
      return PluginProviderResult(PluginProviderResult.Type.PLUGIN, it)
    }
    findPluginByModule(pluginIdOrModuleId)?.let {
      return PluginProviderResult(PluginProviderResult.Type.MODULE, it)
    }
    return null
  }

  override fun getIdePath() = idePath

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
    ): InflatableProductInfoBasedIde {
      return InflatableProductInfoBasedIde(idePath, version, productInfo, pluginCollectionProviders)
    }
  }
}