package com.jetbrains.pluginverifier.tasks.deprecatedUsages

import com.jetbrains.pluginverifier.output.OutputOptions
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.results.Verdict
import com.jetbrains.pluginverifier.tasks.TaskResult
import com.jetbrains.pluginverifier.tasks.TaskResultPrinter

/**
 * @author Sergey Patrikeev
 */
class DeprecatedUsagesResultPrinter(val outputOptions: OutputOptions, val pluginRepository: PluginRepository) : TaskResultPrinter {

  override fun printResults(taskResult: TaskResult) {
    with(taskResult as DeprecatedUsagesResult) {
      for (result in results) {
        val deprecatedApiUsages = with(result.verdict) {
          when (this) {
            is Verdict.OK -> deprecatedUsages
            is Verdict.Warnings -> deprecatedUsages
            is Verdict.MissingDependencies -> deprecatedUsages
            is Verdict.Problems -> deprecatedUsages
            is Verdict.NotFound -> emptySet()
            is Verdict.FailedToDownload -> emptySet()
            is Verdict.Bad -> emptySet()
          }
        }

        if (deprecatedApiUsages.isNotEmpty()) {
          println("In ${result.plugin}:")
          deprecatedApiUsages.forEach {
            println("  $it")
          }
        }
      }
    }
  }
}