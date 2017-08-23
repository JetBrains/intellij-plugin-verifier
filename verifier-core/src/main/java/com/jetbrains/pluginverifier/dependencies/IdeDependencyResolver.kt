package com.jetbrains.pluginverifier.dependencies

import com.intellij.structure.ide.Ide
import com.intellij.structure.plugin.PluginDependency

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