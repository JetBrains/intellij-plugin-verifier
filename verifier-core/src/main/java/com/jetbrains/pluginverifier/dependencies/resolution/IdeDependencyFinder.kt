package com.jetbrains.pluginverifier.dependencies.resolution

import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.pluginverifier.dependencies.resolution.repository.LastCompatibleSelector
import com.jetbrains.pluginverifier.plugin.PluginDetailsProvider
import com.jetbrains.pluginverifier.repository.PluginRepository

/**
 * @author Sergey Patrikeev
 */
class IdeDependencyFinder(ide: Ide, pluginRepository: PluginRepository, pluginDetailsProvider: PluginDetailsProvider) : DependencyFinder {
  private val bundledResolver = BundledPluginDependencyFinder(ide, pluginDetailsProvider)

  private val repositoryDependencyFinder = RepositoryDependencyFinder(pluginRepository, LastCompatibleSelector(ide.version), pluginDetailsProvider)

  override fun findPluginDependency(dependency: PluginDependency): DependencyFinder.Result {
    val result = bundledResolver.findPluginDependency(dependency)
    if (result is DependencyFinder.Result.NotFound) {
      return repositoryDependencyFinder.findPluginDependency(dependency)
    }
    return result
  }


}