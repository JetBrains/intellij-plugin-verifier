package com.jetbrains.pluginverifier.dependencies.resolution

import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.repository.PluginRepository

/**
 * [DependencyFinder] that searches for the dependency in the [PluginRepository].
 * The [pluginVersionSelector] is used to select a specific version of the plugin
 * if multiple versions are available.
 */
class RepositoryDependencyFinder(private val pluginRepository: PluginRepository, private val pluginVersionSelector: PluginVersionSelector, private val pluginDetailsCache: PluginDetailsCache) : DependencyFinder {

  override fun findPluginDependency(dependency: PluginDependency): DependencyFinder.Result {
    if (dependency.isModule) {
      return resolveModuleDependency(dependency.id)
    }
    return selectPluginVersion(dependency.id)
  }

  private fun resolveModuleDependency(moduleId: String): DependencyFinder.Result {
    val pluginId = pluginRepository.getIdOfPluginDeclaringModule(moduleId)
        ?: return DependencyFinder.Result.NotFound("Module '$moduleId' is not found")
    return selectPluginVersion(pluginId)
  }

  private fun selectPluginVersion(pluginId: String): DependencyFinder.Result {
    val selectResult = pluginVersionSelector.selectPluginVersion(pluginId, pluginRepository)
    return when (selectResult) {
      is PluginVersionSelector.Result.Selected -> DependencyFinder.Result.DetailsProvided(pluginDetailsCache.getPluginDetailsCacheEntry(selectResult.pluginInfo))
      is PluginVersionSelector.Result.NotFound -> DependencyFinder.Result.NotFound(selectResult.reason)
    }
  }
}