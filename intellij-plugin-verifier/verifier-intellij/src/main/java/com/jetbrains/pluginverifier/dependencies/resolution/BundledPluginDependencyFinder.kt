package com.jetbrains.pluginverifier.dependencies.resolution

import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.repository.repositories.bundled.BundledPluginsRepository

/**
 * [DependencyFinder] that searches for plugins among bundled plugins of the [ide].
 */
class BundledPluginDependencyFinder(val ide: Ide, private val pluginDetailsCache: PluginDetailsCache) : DependencyFinder {
  private val bundledPluginsRepository = BundledPluginsRepository(ide)

  override val presentableName
    get() = "Bundled plugins of ${ide.version.asString()}"

  override fun findPluginDependency(dependencyId: String, isModule: Boolean): DependencyFinder.Result {
    val bundledPluginInfo = if (isModule) {
      bundledPluginsRepository.findPluginByModule(dependencyId)
    } else {
      bundledPluginsRepository.findPluginById(dependencyId)
    }

    if (bundledPluginInfo != null) {
      return DependencyFinder.Result.DetailsProvided(pluginDetailsCache.getPluginDetailsCacheEntry(bundledPluginInfo))
    }
    return DependencyFinder.Result.NotFound("Dependency $dependencyId is not found among the bundled plugins of $ide")
  }

}