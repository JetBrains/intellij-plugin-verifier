package com.jetbrains.pluginverifier.tasks.checkPlugin

import com.jetbrains.pluginverifier.ide.IdeRepository
import com.jetbrains.pluginverifier.output.OutputOptions
import com.jetbrains.pluginverifier.plugin.PluginDetailsProvider
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.tasks.TaskParameters
import com.jetbrains.pluginverifier.tasks.TaskResultPrinter
import com.jetbrains.pluginverifier.tasks.TaskRunner

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

  override fun createTaskResultsPrinter(outputOptions: OutputOptions, pluginRepository: PluginRepository): TaskResultPrinter =
      CheckPluginResultPrinter(outputOptions, pluginRepository)

}
