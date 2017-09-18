package com.jetbrains.pluginverifier.dependencies

import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.UpdateInfo

interface DependencySelector {
  sealed class Result {
    data class Plugin(val updateInfo: UpdateInfo) : Result()
    data class NotFound(val reason: String) : Result()
  }

  fun select(pluginId: String, pluginRepository: PluginRepository): Result
}