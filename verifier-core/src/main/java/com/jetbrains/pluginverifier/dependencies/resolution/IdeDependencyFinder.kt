package com.jetbrains.pluginverifier.dependencies.resolution

import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.repository.PluginRepository

/**
 * [DependencyFinder] that searches for the
 * dependency among the [bundled] [Ide.getBundledPlugins] [ide] plugins,
 * or the [last compatible] [LastCompatibleVersionSelector] plugin in the [PluginRepository].
 */
class IdeDependencyFinder(private val ide: Ide,
                          private val pluginRepository: PluginRepository,
                          pluginDetailsCache: PluginDetailsCache) : DependencyFinder {

  private val bundledPluginFinder = BundledPluginDependencyFinder(ide, pluginDetailsCache)

  private val repositoryDependencyFinder = RepositoryDependencyFinder(
      pluginRepository,
      LastCompatibleVersionSelector(ide.version),
      pluginDetailsCache
  )

  override fun findPluginDependency(dependency: PluginDependency): DependencyFinder.Result {
    val bundledPlugin = bundledPluginFinder.findPluginDependency(dependency)
    if (bundledPlugin !is DependencyFinder.Result.NotFound) {
      return bundledPlugin
    }
    val repositoryPlugin = repositoryDependencyFinder.findPluginDependency(dependency)
    if (repositoryPlugin !is DependencyFinder.Result.NotFound) {
      return repositoryPlugin
    }
    return DependencyFinder.Result.NotFound("Dependency $dependency is neither resolved among bundled plugins of ${ide.version}, " +
        "nor is there compatible version available in the Plugin Repository ${pluginRepository.repositoryURL}")
  }

}