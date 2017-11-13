package com.jetbrains.pluginverifier.tasks.checkTrunkApi

import com.jetbrains.pluginverifier.ide.IdeRepository
import com.jetbrains.pluginverifier.output.OutputOptions
import com.jetbrains.pluginverifier.plugin.PluginDetailsProvider
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.tasks.TaskParameters
import com.jetbrains.pluginverifier.tasks.TaskResultPrinter
import com.jetbrains.pluginverifier.tasks.TaskRunner

class CheckTrunkApiRunner : TaskRunner() {
  override val commandName: String = "check-trunk-api"

  override fun getParametersBuilder(
      pluginRepository: PluginRepository,
      ideRepository: IdeRepository,
      pluginDetailsProvider: PluginDetailsProvider
  ) = CheckTrunkApiParamsBuilder(pluginRepository, ideRepository)

  override fun createTask(
      parameters: TaskParameters,
      pluginRepository: PluginRepository,
      pluginDetailsProvider: PluginDetailsProvider
  ) = CheckTrunkApiTask(parameters as CheckTrunkApiParams, pluginRepository, pluginDetailsProvider)

  override fun createTaskResultsPrinter(outputOptions: OutputOptions, pluginRepository: PluginRepository): TaskResultPrinter =
      CheckTrunkApiResultPrinter(outputOptions, pluginRepository)

}
