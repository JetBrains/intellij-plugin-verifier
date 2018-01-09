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
class IdeDependencyFinder(ide: Ide,
                          pluginRepository: PluginRepository,
                          pluginDetailsCache: PluginDetailsCache) : DependencyFinder {

  private val bundledPluginFinder = BundledPluginDependencyFinder(ide)

  private val repositoryDependencyFinder = RepositoryDependencyFinder(
      pluginRepository,
      LastCompatibleVersionSelector(ide.version),
      pluginDetailsCache
  )

  private val dependencyFinder = ChainDependencyFinder(listOf(bundledPluginFinder, repositoryDependencyFinder))

  override fun findPluginDependency(dependency: PluginDependency) =
      dependencyFinder.findPluginDependency(dependency)

}