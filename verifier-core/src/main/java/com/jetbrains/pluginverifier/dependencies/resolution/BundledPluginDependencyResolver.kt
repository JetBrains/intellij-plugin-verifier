package com.jetbrains.pluginverifier.dependencies.resolution

import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency

class BundledPluginDependencyResolver(val ide: Ide) : DependencyResolver {

  override fun findPluginDependency(dependency: PluginDependency): DependencyResolver.Result {
    val id = dependency.id
    val existingPlugin = if (dependency.isModule) {
      ide.getPluginByModule(id)
    } else {
      ide.getPluginById(id)
    }
    if (existingPlugin != null) {
      return DependencyResolver.Result.FoundOpenPluginWithoutClasses(existingPlugin)
    }
    return DependencyResolver.Result.NotFound("${dependency.id} is not found in $ide")
  }

}