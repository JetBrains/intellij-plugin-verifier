package com.jetbrains.pluginverifier.tasks

import com.jetbrains.pluginverifier.ide.IdeFilesBank
import com.jetbrains.pluginverifier.output.OutputOptions
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.reporting.PluginVerificationReportage
import com.jetbrains.pluginverifier.repository.PluginRepository

abstract class CommandRunner {

  abstract val commandName: String

  abstract fun getParametersBuilder(
      pluginRepository: PluginRepository,
      ideFilesBank: IdeFilesBank,
      pluginDetailsCache: PluginDetailsCache,
      reportage: PluginVerificationReportage
  ): TaskParametersBuilder

  abstract fun createTask(parameters: TaskParameters, pluginRepository: PluginRepository): Task

  abstract fun createTaskResultsPrinter(
      outputOptions: OutputOptions,
      pluginRepository: PluginRepository
  ): TaskResultPrinter

}