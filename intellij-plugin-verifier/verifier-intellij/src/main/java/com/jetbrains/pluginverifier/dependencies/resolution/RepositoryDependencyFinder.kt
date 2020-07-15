/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.dependencies.resolution

import com.jetbrains.pluginverifier.misc.retry
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.repository.PluginRepository

/**
 * [DependencyFinder] that searches for the dependency in the [PluginRepository].
 * The [pluginVersionSelector] is used to select a specific version of the plugin
 * if multiple versions are available.
 */
class RepositoryDependencyFinder(
  private val pluginRepository: PluginRepository,
  private val pluginVersionSelector: PluginVersionSelector,
  private val pluginDetailsCache: PluginDetailsCache
) : DependencyFinder {

  override val presentableName
    get() = pluginRepository.toString()

  override fun findPluginDependency(dependencyId: String, isModule: Boolean): DependencyFinder.Result =
    retry("Resolve dependency $dependencyId") {
      if (isModule) {
        resolveModuleDependency(dependencyId)
      } else {
        selectPluginVersion(dependencyId)
      }
    }

  private fun resolveModuleDependency(moduleId: String): DependencyFinder.Result {
    val pluginId = pluginRepository.getIdOfPluginDeclaringModule(moduleId)
      ?: return DependencyFinder.Result.NotFound("Module '$moduleId' is not found")
    return selectPluginVersion(pluginId)
  }

  private fun selectPluginVersion(pluginId: String): DependencyFinder.Result {
    return when (val selectResult = pluginVersionSelector.selectPluginVersion(pluginId, pluginRepository)) {
      is PluginVersionSelector.Result.Selected -> {
        val cacheEntryResult = pluginDetailsCache.getPluginDetailsCacheEntry(selectResult.pluginInfo)
        DependencyFinder.Result.DetailsProvided(cacheEntryResult)
      }
      is PluginVersionSelector.Result.NotFound -> DependencyFinder.Result.NotFound(selectResult.reason)
    }
  }
}