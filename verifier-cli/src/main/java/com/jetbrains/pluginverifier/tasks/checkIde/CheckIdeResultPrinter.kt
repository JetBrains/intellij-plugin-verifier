package com.jetbrains.pluginverifier.tasks.checkIde

import com.jetbrains.pluginverifier.misc.create
import com.jetbrains.pluginverifier.output.OutputOptions
import com.jetbrains.pluginverifier.output.html.HtmlResultPrinter
import com.jetbrains.pluginverifier.output.stream.WriterResultPrinter
import com.jetbrains.pluginverifier.output.teamcity.TeamCityLog
import com.jetbrains.pluginverifier.output.teamcity.TeamCityResultPrinter
import com.jetbrains.pluginverifier.parameters.ide.IdeResourceUtil
import com.jetbrains.pluginverifier.repository.PluginIdAndVersion
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.results.Verdict
import com.jetbrains.pluginverifier.tasks.TaskResult
import com.jetbrains.pluginverifier.tasks.TaskResultPrinter
import java.io.File
import java.io.PrintWriter

/**
 * @author Sergey Patrikeev
 */
class CheckIdeResultPrinter(val outputOptions: OutputOptions, val pluginRepository: PluginRepository) : TaskResultPrinter {

  override fun printResults(taskResult: TaskResult) {
    val checkIdeResult = (taskResult as CheckIdeResult)

    if (outputOptions.needTeamCityLog) {
      printTcLog(outputOptions.teamCityGroupType, true, checkIdeResult)
    } else {
      printOnStdOut(checkIdeResult)
    }

    if (outputOptions.htmlReportFile != null) {
      saveToHtmlFile(outputOptions.htmlReportFile, checkIdeResult)
    }

    if (outputOptions.dumpBrokenPluginsFile != null) {
      val brokenPlugins = checkIdeResult.results
          .filter { it.verdict !is Verdict.OK && it.verdict !is Verdict.Warnings }
          .map { it.plugin }
          .map { PluginIdAndVersion(it.pluginId, it.version) }
          .distinct()
      IdeResourceUtil.dumbBrokenPluginsList(File(outputOptions.dumpBrokenPluginsFile), brokenPlugins)
    }
  }

  fun saveToHtmlFile(htmlFile: File, checkIdeResult: CheckIdeResult) {
    with(checkIdeResult) {
      HtmlResultPrinter(listOf(ideVersion), { (pluginId, pluginVersion) ->
        PluginIdAndVersion(pluginId, pluginVersion) in excludedPlugins
      }, htmlFile.create(), outputOptions.missingDependencyIgnoring
      ).printResults(results)
    }
  }

  private fun printTcLog(groupBy: TeamCityResultPrinter.GroupBy,
                         setBuildStatus: Boolean,
                         checkIdeResult: CheckIdeResult) {
    with(checkIdeResult) {
      val tcLog = TeamCityLog(System.out)
      val resultPrinter = TeamCityResultPrinter(tcLog, groupBy, pluginRepository, outputOptions.missingDependencyIgnoring)
      resultPrinter.printResults(results)
      resultPrinter.printNoCompatibleUpdatesProblems(noCompatibleUpdatesProblems)
      if (setBuildStatus) {
        val totalProblemsNumber: Int = results.flatMap {
          val verdict = it.verdict
          when (verdict) {
            is Verdict.Problems -> verdict.problems //some problems might have been caused by missing dependencies
            is Verdict.Bad -> setOf(Any())
            is Verdict.OK, is Verdict.Warnings, is Verdict.NotFound, is Verdict.MissingDependencies, is Verdict.FailedToDownload -> emptySet()
          }
        }.distinct().size
        if (totalProblemsNumber > 0) {
          tcLog.buildStatusFailure("IDE $ideVersion has $totalProblemsNumber problem${if (totalProblemsNumber > 1) "s" else ""}")
        } else {
          tcLog.buildStatusSuccess("IDE $ideVersion doesn't have broken API problems")
        }

      }
    }
  }

  private fun printOnStdOut(checkIdeResult: CheckIdeResult) {
    with(checkIdeResult) {
      val printWriter = PrintWriter(System.out)
      WriterResultPrinter(printWriter, outputOptions.missingDependencyIgnoring).printResults(results)
      printWriter.flush()
    }
  }
}