package com.jetbrains.pluginverifier.tasks.checkPlugin

import com.jetbrains.pluginverifier.ide.IdeFilesBank
import com.jetbrains.pluginverifier.output.OutputOptions
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.reporting.verification.VerificationReportage
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.tasks.CommandRunner
import com.jetbrains.pluginverifier.tasks.TaskParameters
import com.jetbrains.pluginverifier.tasks.TaskResultPrinter

/**
 * [Runner] [CommandRunner] of the ['check-plugin'] [CheckPluginTask] command.
 */
class CheckPluginRunner : CommandRunner() {
  override val commandName: String = "check-plugin"

  override fun getParametersBuilder(
      pluginRepository: PluginRepository,
      ideFilesBank: IdeFilesBank,
      pluginDetailsCache: PluginDetailsCache,
      verificationReportage: VerificationReportage
  ) = CheckPluginParamsBuilder(pluginRepository, verificationReportage)

  override fun createTask(
      parameters: TaskParameters,
      pluginRepository: PluginRepository,
      pluginDetailsCache: PluginDetailsCache
  ) = CheckPluginTask(parameters as CheckPluginParams, pluginRepository, pluginDetailsCache)

  override fun createTaskResultsPrinter(outputOptions: OutputOptions, pluginRepository: PluginRepository): TaskResultPrinter =
      CheckPluginResultPrinter(outputOptions, pluginRepository)

}
