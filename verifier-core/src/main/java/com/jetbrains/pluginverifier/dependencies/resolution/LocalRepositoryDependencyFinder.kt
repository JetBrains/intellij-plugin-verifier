package com.jetbrains.pluginverifier.dependencies.resolution

import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.repository.local.LocalPluginRepository

/**
 * [DependencyFinder] that searches for the [dependency] [PluginDependency]
 * in the [local repository] [LocalPluginRepository].
 */
class LocalRepositoryDependencyFinder(private val localPluginRepository: LocalPluginRepository,
                                      private val pluginDetailsCache: PluginDetailsCache) : DependencyFinder {
  override fun findPluginDependency(dependency: PluginDependency): DependencyFinder.Result {
    val localPlugin = if (dependency.isModule) {
      localPluginRepository.findPluginByModule(dependency.id)
    } else {
      localPluginRepository.findPluginById(dependency.id)
    }
    return if (localPlugin != null) {
      DependencyFinder.Result.DetailsProvided(pluginDetailsCache.getPluginDetails(localPlugin))
    } else {
      DependencyFinder.Result.NotFound("$dependency is not found in the local repository $localPluginRepository")
    }
  }
}