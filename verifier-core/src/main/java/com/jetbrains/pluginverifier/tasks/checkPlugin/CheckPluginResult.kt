package com.jetbrains.pluginverifier.tasks.checkPlugin

import com.jetbrains.pluginverifier.misc.create
import com.jetbrains.pluginverifier.output.html.HtmlResultPrinter
import com.jetbrains.pluginverifier.output.stream.WriterResultPrinter
import com.jetbrains.pluginverifier.output.teamcity.TeamCityLog
import com.jetbrains.pluginverifier.output.teamcity.TeamCityResultPrinter
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.results.Result
import com.jetbrains.pluginverifier.results.Verdict
import com.jetbrains.pluginverifier.tasks.OutputOptions
import com.jetbrains.pluginverifier.tasks.TaskResult
import java.io.File
import java.io.PrintWriter

data class CheckPluginResult(val results: List<Result>) : TaskResult {

  override fun printResults(outputOptions: OutputOptions, pluginRepository: PluginRepository) {
    if (outputOptions.needTeamCityLog) {
      printTcLog(outputOptions, true, pluginRepository)
    } else {
      printOnStdout(outputOptions)
    }

    if (outputOptions.htmlReportFile != null) {
      printToHtml(outputOptions.htmlReportFile, outputOptions)
    }
  }

  private fun printTcLog(outputOptions: OutputOptions, setBuildStatus: Boolean, pluginRepository: PluginRepository) {
    val tcLog = TeamCityLog(System.out)
    val vPrinter = TeamCityResultPrinter(tcLog, outputOptions.teamCityGroupType, pluginRepository, outputOptions.missingDependencyIgnoring)
    vPrinter.printResults(results)
    if (setBuildStatus) {
      val totalProblemsNumber = results.flatMap {
        when (it.verdict) {
          is Verdict.Problems -> it.verdict.problems
          is Verdict.MissingDependencies -> it.verdict.problems  //some problems might have been caused by missing dependencies
          is Verdict.Bad -> setOf(Any())
          is Verdict.OK, is Verdict.Warnings, is Verdict.NotFound, is Verdict.FailedToDownload -> emptySet()
        }
      }.distinct().size
      if (totalProblemsNumber > 0) {
        tcLog.buildStatusFailure("$totalProblemsNumber problem${if (totalProblemsNumber > 0) "s" else ""} found")
      }
    }
  }

  private fun printOnStdout(outputOptions: OutputOptions) {
    val printWriter = PrintWriter(System.out)
    WriterResultPrinter(printWriter, outputOptions.missingDependencyIgnoring).printResults(results)
    printWriter.flush()
  }

  private fun printToHtml(file: File, outputOptions: OutputOptions) {
    val ideVersions = results.map { it.ideVersion }.distinct()
    HtmlResultPrinter(ideVersions, { false }, file.create(), outputOptions.missingDependencyIgnoring).printResults(results)
  }

}
