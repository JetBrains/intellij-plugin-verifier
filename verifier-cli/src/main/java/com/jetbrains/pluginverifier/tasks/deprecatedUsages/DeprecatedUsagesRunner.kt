package com.jetbrains.pluginverifier.tasks.deprecatedUsages

import com.jetbrains.pluginverifier.ide.IdeRepository
import com.jetbrains.pluginverifier.output.OutputOptions
import com.jetbrains.pluginverifier.plugin.PluginDetailsProvider
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.tasks.TaskParameters
import com.jetbrains.pluginverifier.tasks.TaskResultPrinter
import com.jetbrains.pluginverifier.tasks.TaskRunner

class DeprecatedUsagesRunner : TaskRunner() {
  override val commandName: String = "deprecated-usages"

  override fun getParametersBuilder(
      pluginRepository: PluginRepository,
      ideRepository: IdeRepository,
      pluginDetailsProvider: PluginDetailsProvider
  ) = DeprecatedUsagesParamsBuilder(pluginRepository, pluginDetailsProvider)

  override fun createTask(
      parameters: TaskParameters,
      pluginRepository: PluginRepository,
      pluginDetailsProvider: PluginDetailsProvider
  ) = DeprecatedUsagesTask(parameters as DeprecatedUsagesParams, pluginRepository, pluginDetailsProvider)

  override fun createTaskResultsPrinter(outputOptions: OutputOptions, pluginRepository: PluginRepository): TaskResultPrinter =
      DeprecatedUsagesResultPrinter(outputOptions, pluginRepository)

}