/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.dependencies.resolution

import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.intellij.plugin.*
import com.jetbrains.pluginverifier.ide.IdeDescriptor

class IdeThenDelegatePluginProvider(private val ide: Ide, private val delegatePluginProvider: PluginProvider) : PluginProvider {
  override fun findPluginById(pluginId: String): IdePlugin? =
    ide.findPluginById(pluginId)
      ?: delegatePluginProvider.findPluginById(pluginId)

  override fun findPluginByModule(moduleId: String): IdePlugin? =
    ide.findPluginByModule(moduleId)
      ?: delegatePluginProvider.findPluginByModule(moduleId)

  override fun query(query: PluginQuery): PluginProvision {
    if (query.searchId()) {
      ide.findPluginById(query.identifier)?.let {
        return PluginProvision.Found(it, PluginProvision.Source.ID)
      }
    }
    if (query.searchPluginAliases()) {
      ide.findPluginByModule(query.identifier)?.let {
        return PluginProvision.Found(it, PluginProvision.Source.ALIAS)
      }
    }
    if (query.searchContentModuleId()) {
      ide.findPluginByModule(query.identifier)?.let {
        return PluginProvision.Found(it, PluginProvision.Source.CONTENT_MODULE_ID)
      }
    }
    return delegatePluginProvider.query(query)
  }

  companion object {
    fun of(ideDescriptor: IdeDescriptor, dependencyFinder: DependencyFinder, archiveManager: PluginArchiveManager): IdeThenDelegatePluginProvider {
      val ide = ideDescriptor.ide
      val delegate = DependencyFinderPluginProvider(dependencyFinder, ide, archiveManager)
      return IdeThenDelegatePluginProvider(ide, delegate)
    }
  }
}