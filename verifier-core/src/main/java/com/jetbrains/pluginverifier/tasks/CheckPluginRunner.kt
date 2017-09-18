package com.jetbrains.pluginverifier.tasks

import com.jetbrains.pluginverifier.plugin.PluginCreator
import com.jetbrains.pluginverifier.repository.IdeRepository
import com.jetbrains.pluginverifier.repository.PluginRepository

class CheckPluginRunner : TaskRunner() {
  override val commandName: String = "check-plugin"

  override fun getParametersBuilder(
      pluginRepository: PluginRepository,
      ideRepository: IdeRepository,
      pluginCreator: PluginCreator
  ) = CheckPluginParamsBuilder(pluginRepository)

  override fun createTask(
      parameters: TaskParameters,
      pluginRepository: PluginRepository,
      pluginCreator: PluginCreator
  ) = CheckPluginTask(parameters as CheckPluginParams, pluginRepository, pluginCreator)

}