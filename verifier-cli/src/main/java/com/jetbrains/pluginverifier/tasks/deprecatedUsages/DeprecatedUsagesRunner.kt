package com.jetbrains.pluginverifier.tasks.deprecatedUsages

import com.jetbrains.pluginverifier.ide.IdeFilesBank
import com.jetbrains.pluginverifier.output.OutputOptions
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.reporting.verification.VerificationReportage
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
      pluginDetailsCache: PluginDetailsCache, verificationReportage: VerificationReportage
  ) = DeprecatedUsagesParamsBuilder(pluginRepository, pluginDetailsCache)

  override fun createTask(
      parameters: TaskParameters,
      pluginRepository: PluginRepository,
      pluginDetailsCache: PluginDetailsCache
  ) = DeprecatedUsagesTask(parameters as DeprecatedUsagesParams, pluginRepository, pluginDetailsCache)

  override fun createTaskResultsPrinter(outputOptions: OutputOptions, pluginRepository: PluginRepository): TaskResultPrinter =
      DeprecatedUsagesResultPrinter(outputOptions, pluginRepository)

}