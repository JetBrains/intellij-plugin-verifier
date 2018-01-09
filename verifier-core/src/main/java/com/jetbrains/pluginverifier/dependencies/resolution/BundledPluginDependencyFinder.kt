package com.jetbrains.pluginverifier.dependencies.resolution

import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency

/**
 * [DependencyFinder] that searches for the plugin
 * among the [bundled] [Ide.getBundledPlugins] [ide] plugins.
 */
class BundledPluginDependencyFinder(val ide: Ide) : DependencyFinder {

  override fun findPluginDependency(dependency: PluginDependency): DependencyFinder.Result {
    val id = dependency.id
    val existingPlugin = if (dependency.isModule) {
      ide.getPluginByModule(id)
    } else {
      ide.getPluginById(id)
    }
    if (existingPlugin != null) {
      return DependencyFinder.Result.FoundPlugin(existingPlugin)
    }
    return DependencyFinder.Result.NotFound("${dependency.id} is not found in $ide")
  }

}