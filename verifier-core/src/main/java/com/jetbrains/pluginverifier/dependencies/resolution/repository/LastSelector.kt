package com.jetbrains.pluginverifier.dependencies.resolution.repository

import com.jetbrains.pluginverifier.plugin.PluginCoordinate
import com.jetbrains.pluginverifier.repository.PluginRepository

class LastSelector : UpdateSelector {
  override fun select(pluginId: String, pluginRepository: PluginRepository): UpdateSelector.Result {
    val lastUpdate = getLastUpdate(pluginId, pluginRepository) ?: return UpdateSelector.Result.NotFound("Plugin $pluginId is not found in the Plugin Repository")
    return UpdateSelector.Result.Plugin(PluginCoordinate.ByUpdateInfo(lastUpdate, pluginRepository))
  }

  private fun getLastUpdate(pluginId: String, pluginRepository: PluginRepository) = pluginRepository.getAllUpdatesOfPlugin(pluginId)?.maxBy { it.updateId }
}