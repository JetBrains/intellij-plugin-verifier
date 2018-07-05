package com.jetbrains.pluginverifier.tasks.checkTrunkApi

import com.jetbrains.pluginverifier.ide.IdeFilesBank
import com.jetbrains.pluginverifier.output.OutputOptions
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.reporting.verification.Reportage
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.tasks.CommandRunner
import com.jetbrains.pluginverifier.tasks.TaskParameters
import com.jetbrains.pluginverifier.tasks.TaskResultPrinter
import com.jetbrains.pluginverifier.tasks.common.NewProblemsResultPrinter

class CheckTrunkApiRunner : CommandRunner() {
  override val commandName: String = "check-trunk-api"

  override fun getParametersBuilder(
      pluginRepository: PluginRepository,
      ideFilesBank: IdeFilesBank,
      pluginDetailsCache: PluginDetailsCache, reportage: Reportage
  ) = CheckTrunkApiParamsBuilder(pluginRepository, ideFilesBank, reportage)

  override fun createTask(
      parameters: TaskParameters,
      pluginRepository: PluginRepository,
      pluginDetailsCache: PluginDetailsCache
  ) = CheckTrunkApiTask(parameters as CheckTrunkApiParams, pluginRepository, pluginDetailsCache)

  override fun createTaskResultsPrinter(outputOptions: OutputOptions, pluginRepository: PluginRepository): TaskResultPrinter =
      NewProblemsResultPrinter(outputOptions, pluginRepository)

}
