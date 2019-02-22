package com.jetbrains.pluginverifier.tasks.deprecatedUsages

import com.jetbrains.pluginverifier.ide.IdeFilesBank
import com.jetbrains.pluginverifier.output.OutputOptions
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.reporting.verification.Reportage
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.tasks.CommandRunner
import com.jetbrains.pluginverifier.tasks.TaskParameters
import com.jetbrains.pluginverifier.tasks.TaskResultPrinter

/**
 * [Runner] [TaskRunner] of the ['deprecated-usages'] [DeprecatedUsagesTask] command.
 */
class DeprecatedUsagesRunner : CommandRunner() {
  override val commandName: String = "deprecated-usages"

  override fun getParametersBuilder(
      pluginRepository: PluginRepository,
      ideFilesBank: IdeFilesBank,
      pluginDetailsCache: PluginDetailsCache,
      reportage: Reportage
  ) = DeprecatedUsagesParamsBuilder(pluginRepository, pluginDetailsCache, reportage)

  override fun createTask(
      parameters: TaskParameters,
      pluginRepository: PluginRepository
  ) = DeprecatedUsagesTask(parameters as DeprecatedUsagesParams, pluginRepository)

  override fun createTaskResultsPrinter(outputOptions: OutputOptions, pluginRepository: PluginRepository): TaskResultPrinter =
      DeprecatedUsagesResultPrinter(outputOptions, pluginRepository)

}