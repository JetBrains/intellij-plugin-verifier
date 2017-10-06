package com.jetbrains.pluginverifier.dependencies.resolution.repository

import com.jetbrains.pluginverifier.plugin.PluginCoordinate
import com.jetbrains.pluginverifier.repository.PluginRepository

interface UpdateSelector {
  sealed class Result {
    data class Plugin(val updateInfo: PluginCoordinate) : Result()
    data class NotFound(val reason: String) : Result()
  }

  fun select(pluginId: String, pluginRepository: PluginRepository): Result
}