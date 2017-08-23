package com.jetbrains.pluginverifier.dependencies

import com.jetbrains.pluginverifier.repository.RepositoryManager

class LastUpdateSelector : DependencySelector {
  override fun select(pluginId: String): DependencySelector.Result {
    val lastUpdate = getLastUpdate(pluginId) ?: return DependencySelector.Result.NotFound("Plugin $pluginId is not found in the Plugin Repository")
    return DependencySelector.Result.Plugin(lastUpdate)
  }

  private fun getLastUpdate(pluginId: String) = RepositoryManager.getAllUpdatesOfPlugin(pluginId).maxBy { it.updateId }
}