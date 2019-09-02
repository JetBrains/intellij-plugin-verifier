package com.jetbrains.pluginverifier.tasks.checkPlugin

import com.jetbrains.pluginverifier.reporting.PluginVerificationReportage
import com.jetbrains.pluginverifier.ide.IdeFilesBank
import com.jetbrains.pluginverifier.output.OutputOptions
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.tasks.CommandRunner
import com.jetbrains.pluginverifier.tasks.TaskParameters
import com.jetbrains.pluginverifier.tasks.TaskResultPrinter

/**
 * Runner of the ['check-plugin'] [CheckPluginTask] command.
 */
class CheckPluginRunner : CommandRunner() {
  override val commandName: String = "check-plugin"

  override fun getParametersBuilder(
      pluginRepository: PluginRepository,
      ideFilesBank: IdeFilesBank,
      pluginDetailsCache: PluginDetailsCache,
      reportage: PluginVerificationReportage
  ) = CheckPluginParamsBuilder(pluginRepository, reportage, pluginDetailsCache)

  override fun createTask(parameters: TaskParameters, pluginRepository: PluginRepository) =
      CheckPluginTask(parameters as CheckPluginParams)

  override fun createTaskResultsPrinter(outputOptions: OutputOptions, pluginRepository: PluginRepository): TaskResultPrinter =
      CheckPluginResultPrinter(outputOptions, pluginRepository)

}
