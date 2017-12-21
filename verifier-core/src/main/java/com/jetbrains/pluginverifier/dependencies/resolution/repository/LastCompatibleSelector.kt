package com.jetbrains.pluginverifier.dependencies.resolution.repository

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.plugin.PluginCoordinate
import com.jetbrains.pluginverifier.repository.PluginRepository

class LastCompatibleSelector(val ideVersion: IdeVersion) : UpdateSelector {
  override fun select(pluginId: String, pluginRepository: PluginRepository): UpdateSelector.Result {
    val updateInfo = pluginRepository.getLastCompatibleVersionOfPlugin(ideVersion, pluginId)
    if (updateInfo != null) {
      return UpdateSelector.Result.Plugin(PluginCoordinate.ByUpdateInfo(updateInfo, pluginRepository))
    }
    return UpdateSelector.Result.NotFound("Plugin $pluginId doesn't have a build compatible with ${ideVersion}")
  }
}