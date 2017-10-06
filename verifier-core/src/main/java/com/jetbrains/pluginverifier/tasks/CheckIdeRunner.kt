package com.jetbrains.pluginverifier.tasks

import com.jetbrains.pluginverifier.plugin.PluginDetailsProvider
import com.jetbrains.pluginverifier.repository.IdeRepository
import com.jetbrains.pluginverifier.repository.PluginRepository

class CheckIdeRunner : TaskRunner() {
  override val commandName: String = "check-ide"

  override fun getParametersBuilder(
      pluginRepository: PluginRepository,
      ideRepository: IdeRepository,
      pluginDetailsProvider: PluginDetailsProvider
  ) = CheckIdeParamsBuilder(pluginRepository, pluginDetailsProvider)

  override fun createTask(
      parameters: TaskParameters,
      pluginRepository: PluginRepository,
      pluginDetailsProvider: PluginDetailsProvider
  ) = CheckIdeTask(parameters as CheckIdeParams, pluginRepository, pluginDetailsProvider)

}