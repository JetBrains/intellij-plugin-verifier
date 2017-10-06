package com.jetbrains.pluginverifier.dependencies.resolution

import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.pluginverifier.plugin.PluginDetailsProvider

class BundledPluginDependencyFinder(val ide: Ide,
                                    val pluginDetailsProvider: PluginDetailsProvider) : DependencyFinder {

  override fun findPluginDependency(dependency: PluginDependency): DependencyFinder.Result {
    val id = dependency.id
    val existingPlugin = if (dependency.isModule) {
      ide.getPluginByModule(id)
    } else {
      ide.getPluginById(id)
    }
    if (existingPlugin != null) {
      return if (existingPlugin.originalFile != null) {
        DependencyFinder.Result.PluginAndDetailsProvider(existingPlugin, pluginDetailsProvider)
      } else {
        DependencyFinder.Result.FoundOpenPluginWithoutClasses(existingPlugin)
      }
    }
    return DependencyFinder.Result.NotFound("${dependency.id} is not found in $ide")
  }

}