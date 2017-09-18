package com.jetbrains.pluginverifier.dependencies

import com.jetbrains.pluginverifier.repository.PluginRepository

class LastUpdateSelector : DependencySelector {
  override fun select(pluginId: String, pluginRepository: PluginRepository): DependencySelector.Result {
    val lastUpdate = getLastUpdate(pluginId, pluginRepository) ?: return DependencySelector.Result.NotFound("Plugin $pluginId is not found in the Plugin Repository")
    return DependencySelector.Result.Plugin(lastUpdate)
  }

  private fun getLastUpdate(pluginId: String, pluginRepository: PluginRepository) = pluginRepository.getAllUpdatesOfPlugin(pluginId)?.maxBy { it.updateId }
}