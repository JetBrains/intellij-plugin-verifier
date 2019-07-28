package com.jetbrains.pluginverifier.tasks.checkPluginApi

import com.jetbrains.pluginverifier.reporting.PluginVerificationReportage
import com.jetbrains.pluginverifier.ide.IdeFilesBank
import com.jetbrains.pluginverifier.output.OutputOptions
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.tasks.CommandRunner
import com.jetbrains.pluginverifier.tasks.TaskParameters
import com.jetbrains.pluginverifier.tasks.twoTargets.TwoTargetsResultPrinter

/**
 * Runner of the ['check-plugin-api'] [CheckPluginApiTask] command.
 */
class CheckPluginApiRunner : CommandRunner() {
  override val commandName: String = "check-plugin-api"

  override fun getParametersBuilder(
      pluginRepository: PluginRepository,
      ideFilesBank: IdeFilesBank,
      pluginDetailsCache: PluginDetailsCache,
      reportage: PluginVerificationReportage
  ) = CheckPluginApiParamsBuilder(pluginRepository, pluginDetailsCache.pluginDetailsProvider, reportage)

  override fun createTask(parameters: TaskParameters, pluginRepository: PluginRepository) =
      CheckPluginApiTask(parameters as CheckPluginApiParams)

  override fun createTaskResultsPrinter(
      outputOptions: OutputOptions,
      pluginRepository: PluginRepository
  ) = TwoTargetsResultPrinter(outputOptions)

}
