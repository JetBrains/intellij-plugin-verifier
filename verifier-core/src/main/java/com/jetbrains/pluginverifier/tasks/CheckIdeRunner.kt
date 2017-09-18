package com.jetbrains.pluginverifier.tasks

import com.jetbrains.pluginverifier.plugin.PluginCreator
import com.jetbrains.pluginverifier.repository.IdeRepository
import com.jetbrains.pluginverifier.repository.PluginRepository

class CheckIdeRunner : TaskRunner() {
  override val commandName: String = "check-ide"

  override fun getParametersBuilder(
      pluginRepository: PluginRepository,
      ideRepository: IdeRepository,
      pluginCreator: PluginCreator
  ) = CheckIdeParamsBuilder(pluginRepository, pluginCreator)

  override fun createTask(
      parameters: TaskParameters,
      pluginRepository: PluginRepository,
      pluginCreator: PluginCreator
  ) = CheckIdeTask(parameters as CheckIdeParams, pluginRepository, pluginCreator)

}