package com.jetbrains.pluginverifier.tasks

import com.jetbrains.pluginverifier.plugin.PluginDetailsProvider
import com.jetbrains.pluginverifier.repository.IdeRepository
import com.jetbrains.pluginverifier.repository.PluginRepository

class CheckTrunkApiRunner : TaskRunner() {
  override val commandName: String = "check-trunk-api"

  override fun getParametersBuilder(
      pluginRepository: PluginRepository,
      ideRepository: IdeRepository,
      pluginDetailsProvider: PluginDetailsProvider
  ) = CheckTrunkApiParamsBuilder(ideRepository)

  override fun createTask(
      parameters: TaskParameters,
      pluginRepository: PluginRepository,
      pluginDetailsProvider: PluginDetailsProvider
  ) = CheckTrunkApiTask(parameters as CheckTrunkApiParams, pluginRepository, pluginDetailsProvider)

}