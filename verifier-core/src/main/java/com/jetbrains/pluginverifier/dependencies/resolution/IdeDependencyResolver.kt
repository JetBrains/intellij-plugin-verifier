package com.jetbrains.pluginverifier.dependencies.resolution

import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.pluginverifier.dependencies.resolution.repository.LastCompatibleSelector
import com.jetbrains.pluginverifier.plugin.PluginDetailsProvider
import com.jetbrains.pluginverifier.repository.PluginRepository

/**
 * @author Sergey Patrikeev
 */
class IdeDependencyResolver(ide: Ide, pluginRepository: PluginRepository, pluginDetailsProvider: PluginDetailsProvider) : DependencyResolver {
  private val bundledResolver = BundledPluginDependencyResolver(ide)

  private val downloadResolver = RepositoryDependencyResolver(pluginRepository, LastCompatibleSelector(ide.version), pluginDetailsProvider)

  override fun findPluginDependency(dependency: PluginDependency): DependencyResolver.Result {
    val result = bundledResolver.findPluginDependency(dependency)
    if (result is DependencyResolver.Result.NotFound) {
      return downloadResolver.findPluginDependency(dependency)
    }
    return result
  }


}