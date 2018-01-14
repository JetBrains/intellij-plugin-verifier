package com.jetbrains.pluginverifier.dependencies.resolution

import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.PluginRepository

/**
 * [Selects] [selectPluginVersion] a specific [version] [PluginInfo]
 * of the plugin among all plugins available in the [PluginRepository].
 */
interface PluginVersionSelector {

  fun selectPluginVersion(pluginId: String, pluginRepository: PluginRepository): Result

  sealed class Result {
    data class Selected(val pluginInfo: PluginInfo) : Result()

    data class NotFound(val reason: String) : Result()
  }

}