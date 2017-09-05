package com.jetbrains.pluginverifier.dependencies

import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency

/**
 * @author Sergey Patrikeev
 */
class IdeDependencyResolver(val ide: Ide) : DependencyResolver {
  private val bundledResolver = BundledPluginDependencyResolver(ide)

  private val downloadResolver = DownloadDependencyResolver(LastCompatibleSelector(ide.version))

  override fun resolve(dependency: PluginDependency): DependencyResolver.Result {
    val result = bundledResolver.resolve(dependency)
    if (result is DependencyResolver.Result.NotFound) {
      return downloadResolver.resolve(dependency)
    }
    return result
  }


}