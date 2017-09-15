package com.jetbrains.pluginverifier

import com.jetbrains.pluginverifier.plugin.PluginCreator
import com.jetbrains.pluginverifier.repository.IdeRepository
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.tasks.CheckTrunkApiParams
import com.jetbrains.pluginverifier.tasks.CheckTrunkApiParamsBuilder
import com.jetbrains.pluginverifier.tasks.CheckTrunkApiTask
import com.jetbrains.pluginverifier.tasks.TaskParameters

class CheckTrunkApiRunner : TaskRunner() {
  override val commandName: String = "check-trunk-api"

  override fun getParametersBuilder(
      pluginRepository: PluginRepository,
      ideRepository: IdeRepository,
      pluginCreator: PluginCreator
  ) = CheckTrunkApiParamsBuilder(ideRepository)

  override fun createTask(
      parameters: TaskParameters,
      pluginRepository: PluginRepository,
      pluginCreator: PluginCreator
  ) = CheckTrunkApiTask(parameters as CheckTrunkApiParams, pluginRepository, pluginCreator)

}