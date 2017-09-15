package com.jetbrains.pluginverifier

import com.jetbrains.pluginverifier.plugin.PluginCreator
import com.jetbrains.pluginverifier.repository.IdeRepository
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.tasks.CheckPluginParams
import com.jetbrains.pluginverifier.tasks.CheckPluginParamsBuilder
import com.jetbrains.pluginverifier.tasks.CheckPluginTask
import com.jetbrains.pluginverifier.tasks.TaskParameters

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