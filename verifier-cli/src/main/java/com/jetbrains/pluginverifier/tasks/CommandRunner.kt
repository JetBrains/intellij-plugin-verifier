package com.jetbrains.pluginverifier.tasks

import com.jetbrains.pluginverifier.ide.IdeFilesBank
import com.jetbrains.pluginverifier.output.OutputOptions
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.reporting.verification.VerificationReportage
import com.jetbrains.pluginverifier.repository.PluginRepository

/**
 * Runner of a verification command that:
 * 1) [Provides] [getParametersBuilder] the [TaskParametersBuilder]
 * used to build the [TaskParameters] of this specific task.
 * 2) [Creates] [createTask] the verification [task] [Task] to run.
 * 3) [Creates] [createTaskResultsPrinter] a [TaskResultPrinter]
 * used to print the verification result.
 */
abstract class CommandRunner {

  abstract val commandName: String

  abstract fun getParametersBuilder(
      pluginRepository: PluginRepository,
      ideFilesBank: IdeFilesBank,
      pluginDetailsCache: PluginDetailsCache,
      verificationReportage: VerificationReportage
  ): TaskParametersBuilder

  abstract fun createTask(
      parameters: TaskParameters,
      pluginRepository: PluginRepository,
      pluginDetailsCache: PluginDetailsCache
  ): Task

  abstract fun createTaskResultsPrinter(
      outputOptions: OutputOptions,
      pluginRepository: PluginRepository
  ): TaskResultPrinter

}