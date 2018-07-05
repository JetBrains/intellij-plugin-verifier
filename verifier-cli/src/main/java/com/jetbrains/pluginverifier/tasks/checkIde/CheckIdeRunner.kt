package com.jetbrains.pluginverifier.tasks.checkIde

import com.jetbrains.pluginverifier.ide.IdeFilesBank
import com.jetbrains.pluginverifier.output.OutputOptions
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.reporting.verification.Reportage
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.tasks.CommandRunner
import com.jetbrains.pluginverifier.tasks.TaskParameters
import com.jetbrains.pluginverifier.tasks.TaskResultPrinter

/**
 * [Runner] [CommandRunner] of the ['check-ide'] [CheckIdeTask] command.
 */
class CheckIdeRunner : CommandRunner() {
  override val commandName: String = "check-ide"

  override fun getParametersBuilder(
      pluginRepository: PluginRepository,
      ideFilesBank: IdeFilesBank,
      pluginDetailsCache: PluginDetailsCache,
      reportage: Reportage
  ) = CheckIdeParamsBuilder(pluginRepository, pluginDetailsCache, reportage)

  override fun createTask(
      parameters: TaskParameters,
      pluginRepository: PluginRepository,
      pluginDetailsCache: PluginDetailsCache
  ) = CheckIdeTask(parameters as CheckIdeParams)

  override fun createTaskResultsPrinter(outputOptions: OutputOptions, pluginRepository: PluginRepository): TaskResultPrinter =
      CheckIdeResultPrinter(outputOptions, pluginRepository)

}