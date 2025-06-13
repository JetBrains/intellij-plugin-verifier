/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.dependencies.resolution

import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.PluginProvider
import com.jetbrains.plugin.structure.intellij.plugin.PluginProvision
import com.jetbrains.plugin.structure.intellij.plugin.PluginQuery
import com.jetbrains.pluginverifier.dependencies.resolution.DependencyOrigin.Bundled
import com.jetbrains.pluginverifier.plugin.PluginDetails

class DependencyFinderPluginProvider(private val dependencyFinder: DependencyFinder, private val ide: Ide) : PluginProvider {
  override fun findPluginById(pluginId: String): IdePlugin? {
    return dependencyFinder
      .findPluginDependency(pluginId, isModule = false)
      .getPlugin()
  }

  override fun findPluginByModule(moduleId: String): IdePlugin? {
    return dependencyFinder
      .findPluginDependency(moduleId, isModule = true)
      .getPlugin()
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

  private fun DependencyFinder.Result.getPlugin(): IdePlugin? {
    val pluginDetails = when (this) {
      is DependencyFinder.Result.DetailsProvided -> this.getDetails()
      is DependencyFinder.Result.FoundPlugin -> this.getDetails()
      is DependencyFinder.Result.NotFound -> null
    }
    return pluginDetails?.idePlugin
  }

  // FIXME duplicate with DefaultClassResolverProvider
  private fun DependencyFinder.Result.FoundPlugin.getDetails(): PluginDetails {
    return if (origin == Bundled) {
      getBundledPluginDetails(ide, plugin)
    } else {
      getNonBundledDependencyDetails(plugin)
    }
  }
}
