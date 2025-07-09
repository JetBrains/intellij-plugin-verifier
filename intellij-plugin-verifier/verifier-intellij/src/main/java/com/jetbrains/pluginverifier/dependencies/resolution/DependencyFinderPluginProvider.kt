/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.dependencies.resolution

import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.intellij.classes.plugin.ClassSearchContext
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.PluginArchiveManager
import com.jetbrains.plugin.structure.intellij.plugin.PluginProvider
import com.jetbrains.plugin.structure.intellij.plugin.PluginProvision
import com.jetbrains.plugin.structure.intellij.plugin.PluginQuery

/**
 * A dependency resolution bridge between IntelliJ Plugin Verifier [DependencyFinder] and IntelliJ Structure Library [PluginProvider].
 *
 * This allows using JetBrains Marketplace repositories or local repositories as sources for plugin resolution within the
 * "Structure" library.
 */
class DependencyFinderPluginProvider(private val dependencyFinder: DependencyFinder, private val ide: Ide, val archiveManager: PluginArchiveManager) : PluginProvider {
  private val classSearchContext = ClassSearchContext(archiveManager)

  override fun findPluginById(pluginId: String): IdePlugin? {
    return dependencyFinder
      .findPluginDependency(pluginId, isModule = false)
      .resolvePlugin(ide, classSearchContext)
  }

  override fun findPluginByModule(moduleId: String): IdePlugin? {
    return dependencyFinder
      .findPluginDependency(moduleId, isModule = true)
      .resolvePlugin(ide, classSearchContext)
  }

  override fun query(query: PluginQuery): PluginProvision {
    if (query.searchId()) {
      findPluginById(query.identifier)?.let {
        return PluginProvision.Found(it, PluginProvision.Source.ID)
      }
    } else if (query.searchPluginAliases()) {
      findPluginByModule(query.identifier)?.let {
        return PluginProvision.Found(it, PluginProvision.Source.ALIAS)
      }
    } else if (query.searchContentModuleId()) {
      findPluginByModule(query.identifier)?.let {
        return PluginProvision.Found(it, PluginProvision.Source.CONTENT_MODULE_ID)
      }
    }
    return PluginProvision.NotFound
  }
}
