package com.jetbrains.pluginverifier.tasks.checkIde

import com.jetbrains.pluginverifier.ide.IdeRepository
import com.jetbrains.pluginverifier.output.OutputOptions
import com.jetbrains.pluginverifier.plugin.PluginDetailsProvider
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.tasks.TaskParameters
import com.jetbrains.pluginverifier.tasks.TaskResultPrinter
import com.jetbrains.pluginverifier.tasks.TaskRunner

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

  override fun createTaskResultsPrinter(outputOptions: OutputOptions, pluginRepository: PluginRepository): TaskResultPrinter =
      CheckIdeResultPrinter(outputOptions, pluginRepository)

}