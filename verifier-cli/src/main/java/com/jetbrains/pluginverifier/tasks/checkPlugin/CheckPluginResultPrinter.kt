package com.jetbrains.pluginverifier.tasks.checkPlugin

import com.jetbrains.pluginverifier.output.OutputOptions
import com.jetbrains.pluginverifier.output.stream.WriterResultPrinter
import com.jetbrains.pluginverifier.output.teamcity.TeamCityLog
import com.jetbrains.pluginverifier.output.teamcity.TeamCityResultPrinter
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.results.Verdict
import com.jetbrains.pluginverifier.tasks.TaskResult
import com.jetbrains.pluginverifier.tasks.TaskResultPrinter
import java.io.PrintWriter

/**
 * @author Sergey Patrikeev
 */
class CheckPluginResultPrinter(private val outputOptions: OutputOptions,
                               private val pluginRepository: PluginRepository) : TaskResultPrinter {

  override fun printResults(taskResult: TaskResult) {
    with(taskResult as CheckPluginResult) {
      if (outputOptions.needTeamCityLog) {
        printTcLog(true, this)
      } else {
        printOnStdout(this)
      }

      results.groupBy { it.ideVersion }.forEach { ideVersion, resultsOfIde ->
        outputOptions.saveToHtmlFile(ideVersion, emptyList(), resultsOfIde)
      }
    }
  }

  private fun printTcLog(setBuildStatus: Boolean, checkPluginResult: CheckPluginResult) {
    with(checkPluginResult) {
      val tcLog = TeamCityLog(System.out)
      val vPrinter = TeamCityResultPrinter(tcLog, outputOptions.teamCityGroupType, pluginRepository, outputOptions.missingDependencyIgnoring)
      vPrinter.printResults(results)
      if (setBuildStatus) {
        val totalProblemsNumber = results.flatMap {
          val verdict = it.verdict
          when (verdict) {
            is Verdict.Problems -> verdict.problems
            is Verdict.MissingDependencies -> verdict.problems  //some problems might have been caused by missing dependencies
            is Verdict.Bad -> setOf(Any())
            is Verdict.OK, is Verdict.Warnings, is Verdict.NotFound, is Verdict.FailedToDownload -> emptySet()
          }
        }.distinct().size
        if (totalProblemsNumber > 0) {
          tcLog.buildStatusFailure("$totalProblemsNumber problem${if (totalProblemsNumber > 0) "s" else ""} found")
        }
      }
    }
  }

  private fun printOnStdout(checkPluginResult: CheckPluginResult) {
    with(checkPluginResult) {
      val printWriter = PrintWriter(System.out)
      WriterResultPrinter(printWriter, outputOptions.missingDependencyIgnoring).printResults(results)
      printWriter.flush()
    }
  }

}