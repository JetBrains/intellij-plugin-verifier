package com.jetbrains.pluginverifier.dependencies

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.repository.PluginRepository

class LastCompatibleSelector(val ideVersion: IdeVersion) : DependencySelector {
  override fun select(pluginId: String, pluginRepository: PluginRepository): DependencySelector.Result {
    val updateInfo = pluginRepository.getLastCompatibleUpdateOfPlugin(ideVersion, pluginId)
    if (updateInfo != null) {
      return DependencySelector.Result.Plugin(updateInfo)
    }
    return DependencySelector.Result.NotFound("Plugin $pluginId doesn't have a build compatible with $ideVersion")
  }
}