package com.jetbrains.pluginverifier.dependencies

import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.RepositoryManager

/**
 * @author Sergey Patrikeev
 */
class IdeDependencyResolver(ide: Ide, pluginRepository: PluginRepository = RepositoryManager) : DependencyResolver {
  private val bundledResolver = BundledPluginDependencyResolver(ide)

  private val downloadResolver = DownloadDependencyResolver(LastCompatibleSelector(ide.version, pluginRepository), pluginRepository)

  override fun resolve(dependency: PluginDependency): DependencyResolver.Result {
    val result = bundledResolver.resolve(dependency)
    if (result is DependencyResolver.Result.NotFound) {
      return downloadResolver.resolve(dependency)
    }
    return result
  }


}