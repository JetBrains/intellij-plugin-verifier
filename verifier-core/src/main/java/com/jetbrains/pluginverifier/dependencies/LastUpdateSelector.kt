package com.jetbrains.pluginverifier.dependencies

import com.jetbrains.pluginverifier.repository.PluginRepository

class LastUpdateSelector(val pluginRepository: PluginRepository) : DependencySelector {
  override fun select(pluginId: String): DependencySelector.Result {
    val lastUpdate = getLastUpdate(pluginId) ?: return DependencySelector.Result.NotFound("Plugin $pluginId is not found in the Plugin Repository")
    return DependencySelector.Result.Plugin(lastUpdate)
  }

  private fun getLastUpdate(pluginId: String) = pluginRepository.getAllUpdatesOfPlugin(pluginId)?.maxBy { it.updateId }
}