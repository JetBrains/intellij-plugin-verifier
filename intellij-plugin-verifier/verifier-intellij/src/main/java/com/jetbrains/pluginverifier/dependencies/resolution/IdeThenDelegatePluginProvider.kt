/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.dependencies.resolution

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.PluginArchiveManager
import com.jetbrains.plugin.structure.intellij.plugin.PluginProvider
import com.jetbrains.plugin.structure.intellij.plugin.PluginProvision
import com.jetbrains.plugin.structure.intellij.plugin.PluginQuery
import com.jetbrains.plugin.structure.intellij.plugin.dependencies.PluginId
import com.jetbrains.pluginverifier.ide.IdeDescriptor

class IdeThenDelegatePluginProvider(private val ide: Ide, private val delegatePluginProvider: PluginProvider) : PluginProvider {
  private val cache: Cache<PluginId, PluginProvision> = Caffeine
    .newBuilder()
    .maximumSize(1)
    .build()

  override fun findPluginById(pluginId: String): IdePlugin? =
    ide.findPluginById(pluginId)
      ?: delegatePluginProvider.findPluginById(pluginId)

  override fun findPluginByModule(moduleId: String): IdePlugin? =
    ide.findPluginByModule(moduleId)
      ?: delegatePluginProvider.findPluginByModule(moduleId)

  override fun query(query: PluginQuery): PluginProvision = cache.get(query.identifier) {
    if (query.searchId()) {
      ide.findPluginById(query.identifier)?.let {
        return@get PluginProvision.Found(it, PluginProvision.Source.ID)
      }
    }
    if (query.searchPluginAliases()) {
      ide.findPluginByModule(query.identifier)?.let {
        return@get PluginProvision.Found(it, PluginProvision.Source.ALIAS)
      }
    }
    if (query.searchContentModuleId()) {
      ide.findPluginByModule(query.identifier)?.let {
        return@get PluginProvision.Found(it, PluginProvision.Source.CONTENT_MODULE_ID)
      }
    }
    return@get delegatePluginProvider.query(query)
  }

  companion object {
    fun of(ideDescriptor: IdeDescriptor, dependencyFinder: DependencyFinder, archiveManager: PluginArchiveManager): IdeThenDelegatePluginProvider {
      val ide = ideDescriptor.ide
      val delegate = DependencyFinderPluginProvider(dependencyFinder, ide, archiveManager)
      return IdeThenDelegatePluginProvider(ide, delegate)
    }
  }
}