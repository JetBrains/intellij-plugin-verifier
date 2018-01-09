package com.jetbrains.pluginverifier.dependencies.resolution

import com.jetbrains.pluginverifier.misc.tryInvokeSeveralTimes
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.UpdateInfo
import java.util.concurrent.TimeUnit

/**
 * [PluginVersionSelector] that selects the _last_ version
 * of the plugin from the [repository] [PluginRepository].
 */
class LastVersionSelector : PluginVersionSelector {
  override fun selectPluginVersion(pluginId: String, pluginRepository: PluginRepository): PluginVersionSelector.Result {
    val lastUpdate = this.tryInvokeSeveralTimes(3, 500, TimeUnit.MILLISECONDS, "fetch last update of $pluginId") {
      getLastUpdate(pluginId, pluginRepository) as? UpdateInfo
    }
    return if (lastUpdate == null) {
      PluginVersionSelector.Result.NotFound("Plugin $pluginId is not found in the Plugin Repository")
    } else {
      PluginVersionSelector.Result.Plugin(lastUpdate)
    }
  }

  private fun getLastUpdate(pluginId: String, pluginRepository: PluginRepository) =
      pluginRepository.getAllVersionsOfPlugin(pluginId).maxBy { (it as UpdateInfo).updateId }
}