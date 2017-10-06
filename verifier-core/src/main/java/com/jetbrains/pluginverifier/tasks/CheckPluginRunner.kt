package com.jetbrains.pluginverifier.tasks

import com.jetbrains.pluginverifier.plugin.PluginDetailsProvider
import com.jetbrains.pluginverifier.repository.IdeRepository
import com.jetbrains.pluginverifier.repository.PluginRepository

class CheckPluginRunner : TaskRunner() {
  override val commandName: String = "check-plugin"

  override fun getParametersBuilder(
      pluginRepository: PluginRepository,
      ideRepository: IdeRepository,
      pluginDetailsProvider: PluginDetailsProvider
  ) = CheckPluginParamsBuilder(pluginRepository)

  override fun createTask(
      parameters: TaskParameters,
      pluginRepository: PluginRepository,
      pluginDetailsProvider: PluginDetailsProvider
  ) = CheckPluginTask(parameters as CheckPluginParams, pluginRepository, pluginDetailsProvider)

}