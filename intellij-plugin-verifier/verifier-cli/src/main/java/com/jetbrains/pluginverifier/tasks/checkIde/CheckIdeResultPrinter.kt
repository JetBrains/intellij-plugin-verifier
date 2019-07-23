package com.jetbrains.pluginverifier.tasks.checkIde

import com.jetbrains.plugin.structure.base.utils.pluralizeWithNumber
import com.jetbrains.pluginverifier.VerificationTarget
import com.jetbrains.pluginverifier.ide.IdeResourceUtil
import com.jetbrains.pluginverifier.output.OutputOptions
import com.jetbrains.pluginverifier.output.html.HtmlResultPrinter
import com.jetbrains.pluginverifier.output.stream.WriterResultPrinter
import com.jetbrains.pluginverifier.output.teamcity.TeamCityLog
import com.jetbrains.pluginverifier.output.teamcity.TeamCityResultPrinter
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.results.VerificationResult
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.tasks.TaskResult
import com.jetbrains.pluginverifier.tasks.TaskResultPrinter
import java.io.File
import java.io.PrintWriter

class CheckIdeResultPrinter(val outputOptions: OutputOptions, val pluginRepository: PluginRepository) : TaskResultPrinter {

  override fun printResults(taskResult: TaskResult) {
    with(taskResult as CheckIdeResult) {
      if (outputOptions.teamCityLog != null) {
        printTcLog(outputOptions.teamCityGroupType, this, outputOptions.teamCityLog)
      } else {
        printOnStdOut(this)
      }

      HtmlResultPrinter(
          VerificationTarget.Ide(ideVersion),
          outputOptions.getTargetReportDirectory(VerificationTarget.Ide(ideVersion)).resolve("report.html")
      ).printResults(results)

      if (outputOptions.dumpBrokenPluginsFile != null) {
        val brokenPlugins = results
            .filter { it !is VerificationResult.OK && it !is VerificationResult.CompatibilityWarnings }
            .map { it.plugin }
            .distinct()
        IdeResourceUtil.dumbBrokenPluginsList(File(outputOptions.dumpBrokenPluginsFile), brokenPlugins)
      }
    }
  }

  private fun printTcLog(groupBy: TeamCityResultPrinter.GroupBy, checkIdeResult: CheckIdeResult, tcLog: TeamCityLog) {
    with(checkIdeResult) {
      val resultPrinter = TeamCityResultPrinter(tcLog, groupBy, pluginRepository)
      resultPrinter.printResults(results)
      resultPrinter.printNoCompatibleVersionsProblems(missingCompatibleVersionsProblems)
      val problems = hashSetOf<CompatibilityProblem>()
      val brokenPlugins = hashSetOf<PluginInfo>()
      for (result in results) {
        when (result) {
          is VerificationResult.CompatibilityProblems -> {
            problems += result.compatibilityProblems
            brokenPlugins += result.plugin
          }
          is VerificationResult.MissingDependencies -> {
            problems += result.compatibilityProblems
            brokenPlugins += result.plugin
          }
          is VerificationResult.OK,
          is VerificationResult.CompatibilityWarnings,
          is VerificationResult.InvalidPlugin,
          is VerificationResult.NotFound,
          is VerificationResult.FailedToDownload -> Unit
        }
      }
      val problemsNumber = problems.distinctBy { it.shortDescription }.size
      if (problemsNumber > 0) {
        tcLog.buildStatusFailure("IDE $ideVersion has " + "problem".pluralizeWithNumber(problemsNumber) + " affecting " + "plugin".pluralizeWithNumber(brokenPlugins.size))
      } else {
        tcLog.buildStatusSuccess("IDE $ideVersion doesn't have broken API problems")
      }
    }
  }

  private fun printOnStdOut(checkIdeResult: CheckIdeResult) {
    val printWriter = PrintWriter(System.out)
    val resultPrinter = WriterResultPrinter(printWriter)
    resultPrinter.printResults(checkIdeResult.results)
    printWriter.flush()
  }
}