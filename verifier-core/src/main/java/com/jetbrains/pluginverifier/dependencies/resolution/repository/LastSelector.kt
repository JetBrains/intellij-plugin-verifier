package com.jetbrains.pluginverifier.dependencies.resolution.repository

import com.jetbrains.pluginverifier.misc.tryInvokeSeveralTimes
import com.jetbrains.pluginverifier.plugin.PluginCoordinate
import com.jetbrains.pluginverifier.repository.PluginRepository
import java.util.concurrent.TimeUnit

class LastSelector : UpdateSelector {
  override fun select(pluginId: String, pluginRepository: PluginRepository): UpdateSelector.Result {
    val lastUpdate = this.tryInvokeSeveralTimes(3, 500, TimeUnit.MILLISECONDS, "fetch last update of $pluginId") {
      getLastUpdate(pluginId, pluginRepository)
    }
    return if (lastUpdate == null) {
      UpdateSelector.Result.NotFound("Plugin $pluginId is not found in the Plugin Repository")
    } else {
      UpdateSelector.Result.Plugin(PluginCoordinate.ByUpdateInfo(lastUpdate, pluginRepository))
    }
  }

  private fun getLastUpdate(pluginId: String, pluginRepository: PluginRepository) =
      pluginRepository.getAllUpdatesOfPlugin(pluginId)?.maxBy { it.updateId }
}