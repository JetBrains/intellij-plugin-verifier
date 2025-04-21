/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.dependencies.resolution

import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.ModuleV2Dependency
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.plugin.structure.intellij.plugin.PluginV2Dependency
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.repository.repositories.bundled.BundledPluginInfo

/**
 * [DependencyFinder] that searches for plugins among bundled plugins of the [ide].
 */
class BundledPluginDependencyFinder(val ide: Ide, private val pluginDetailsCache: PluginDetailsCache) :
  DependencyFinder {
  override val presentableName
    get() = "Bundled plugins of ${ide.version.asString()}"

  override fun findPluginDependency(dependencyId: String, isModule: Boolean): DependencyFinder.Result {
    val bundledPluginInfo = if (isModule) {
      ide.findPluginOrModuleById(dependencyId)
    } else {
      ide.findPluginById(dependencyId)
    }

    return bundledPluginInfo.toResult(dependencyId)
  }

  override fun findPluginDependency(dependency: PluginDependency): DependencyFinder.Result {
    return when (dependency) {
      is ModuleV2Dependency,
      is PluginV2Dependency -> ide.findPluginOrModuleById(dependency.id)
        .toResult(dependency.id)

      else -> findPluginDependency(dependency.id, dependency.isModule)
    }
  }

  private fun IdePlugin?.toResult(dependencyId: String): DependencyFinder.Result {
    return if (this != null) {
      val pluginInfo = BundledPluginInfo(ide.version, this)
      DependencyFinder.Result.DetailsProvided(pluginDetailsCache.getPluginDetailsCacheEntry(pluginInfo))
    } else {
      DependencyFinder.Result.NotFound("Dependency $dependencyId is not found among the bundled plugins of $ide")
    }
  }

  private fun Ide.findPluginOrModuleById(dependencyId: String): IdePlugin? {
    // module can expose itself as a plugin with the corresponding ID
    return findPluginById(dependencyId)
    // if there is no such plugin, search in modules
      ?: findPluginByModule(dependencyId)
  }

}