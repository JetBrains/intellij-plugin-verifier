/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide

import com.jetbrains.plugin.structure.ide.ProductInfoBasedIdeManager.PluginReader
import com.jetbrains.plugin.structure.ide.layout.ProductInfoLayoutComponentNameSource
import com.jetbrains.plugin.structure.intellij.platform.ProductInfo
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

class ProductInfoPluginReaderPluginCollectionProvider(private val pluginReader: PluginReader<ProductInfo>) : TargetedPluginCollectionProvider<Path> {
  private val stateBySource = ConcurrentHashMap<ProductInfoPluginCollectionSource, State>()

  override fun getPlugins(source: PluginCollectionSource<Path, *>): Collection<IdePlugin> {
    return if (source is ProductInfoPluginCollectionSource) {
      state(source).getPlugins()
    } else {
      emptySet()
    }
  }

  override fun findPluginById(source: PluginCollectionSource<Path, *>, pluginId: String): PluginLookupResult {
    return if (source is ProductInfoPluginCollectionSource) {
      state(source).findPluginById(pluginId)
    } else {
      PluginLookupResult.unsupported()
    }
  }

  override fun findPluginByModule(source: PluginCollectionSource<Path, *>, moduleId: String): PluginLookupResult {
    return if (source is ProductInfoPluginCollectionSource) {
      state(source).findPluginByModule(moduleId)
    } else {
      PluginLookupResult.unsupported()
    }
  }

  private fun state(source: ProductInfoPluginCollectionSource): State {
    return stateBySource.computeIfAbsent(source) { State(it) }
  }

  private inner class State(private val source: ProductInfoPluginCollectionSource) {
    private var plugins: Collection<IdePlugin>? = null
    private val pluginsById = hashMapOf<String, IdePlugin>()
    private val pluginsByModuleId = hashMapOf<String, IdePlugin>()

    @Synchronized
    fun getPlugins(): Collection<IdePlugin> {
      return plugins ?: loadAndIndex().also { plugins = it }
    }

    @Synchronized
    fun findPluginById(pluginId: String): PluginLookupResult {
      ensureLoaded()
      val plugin = pluginsById[pluginId]
      return if (plugin != null) PluginLookupResult.found(plugin) else PluginLookupResult.notFound()
    }

    @Synchronized
    fun findPluginByModule(moduleId: String): PluginLookupResult {
      ensureLoaded()
      val plugin = pluginsByModuleId[moduleId]
      return if (plugin != null) PluginLookupResult.found(plugin) else PluginLookupResult.notFound()
    }

    private fun ensureLoaded() {
      if (plugins == null) {
        plugins = loadAndIndex()
      }
    }

    private fun loadAndIndex(): Collection<IdePlugin> {
      val layoutComponentNameSource = ProductInfoLayoutComponentNameSource(source.productInfo)
      val loaded = pluginReader.readPlugins(source.idePath, source.productInfo, layoutComponentNameSource, source.ideVersion)
      for (plugin in loaded) {
        val id = plugin.pluginId ?: plugin.pluginName
        if (id != null) {
          pluginsById.putIfAbsent(id, plugin)
        }
        for (moduleId in plugin.definedModules) {
          pluginsByModuleId.putIfAbsent(moduleId, plugin)
        }
      }
      return loaded
    }
  }
}