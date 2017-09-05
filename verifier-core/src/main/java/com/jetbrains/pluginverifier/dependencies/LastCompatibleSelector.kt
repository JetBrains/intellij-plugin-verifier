package com.jetbrains.pluginverifier.dependencies

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.repository.RepositoryManager

class LastCompatibleSelector(val ideVersion: IdeVersion) : DependencySelector {
  override fun select(pluginId: String): DependencySelector.Result {
    val updateInfo = RepositoryManager.getLastCompatibleUpdateOfPlugin(ideVersion, pluginId)
    if (updateInfo != null) {
      return DependencySelector.Result.Plugin(updateInfo)
    }
    return DependencySelector.Result.NotFound("Plugin $pluginId doesn't have a build compatible with $ideVersion")
  }
}