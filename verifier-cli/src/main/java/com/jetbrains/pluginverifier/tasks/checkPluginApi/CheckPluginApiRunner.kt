package com.jetbrains.pluginverifier.tasks.checkPluginApi

import com.jetbrains.pluginverifier.ide.IdeFilesBank
import com.jetbrains.pluginverifier.output.OutputOptions
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.reporting.verification.VerificationReportage
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.tasks.CommandRunner
import com.jetbrains.pluginverifier.tasks.TaskParameters
import com.jetbrains.pluginverifier.tasks.common.NewProblemsResultPrinter

/**
 * [Runner] [CommandRunner] of the ['check-plugin-api'] [CheckPluginApiTask] command.
 */
class CheckPluginApiRunner : CommandRunner() {
  override val commandName: String = "check-plugin-api"

  override fun getParametersBuilder(
      pluginRepository: PluginRepository,
      ideFilesBank: IdeFilesBank,
      pluginDetailsCache: PluginDetailsCache,
      verificationReportage: VerificationReportage
  ) = CheckPluginApiParamsBuilder(pluginRepository, pluginDetailsCache)

  override fun createTask(
      parameters: TaskParameters,
      pluginRepository: PluginRepository,
      pluginDetailsCache: PluginDetailsCache
  ) = CheckPluginApiTask(parameters as CheckPluginApiParams)

  override fun createTaskResultsPrinter(
      outputOptions: OutputOptions,
      pluginRepository: PluginRepository
  ) = NewProblemsResultPrinter(outputOptions, pluginRepository)

}
