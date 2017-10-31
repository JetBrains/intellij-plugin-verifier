package com.jetbrains.pluginverifier.tasks.deprecatedUsages

import com.jetbrains.pluginverifier.output.OutputOptions
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.tasks.TaskResult
import com.jetbrains.pluginverifier.tasks.TaskResultPrinter

/**
 * @author Sergey Patrikeev
 */
class DeprecatedUsagesResultPrinter(val outputOptions: OutputOptions, val pluginRepository: PluginRepository) : TaskResultPrinter {

  override fun printResults(taskResult: TaskResult) {
    with(taskResult as DeprecatedUsagesResult) {
      for ((plugin, deprecatedApiUsages) in results) {
        if (deprecatedApiUsages.isNotEmpty()) {
          println("Deprecated usages of $plugin:")
          deprecatedApiUsages.groupBy { it.shortDescription }.forEach { (shortDescription, allWithShortDescription) ->
            println("  $shortDescription")
            allWithShortDescription.forEach {
              println("      $it")
            }
          }
        }
      }
    }
  }

}