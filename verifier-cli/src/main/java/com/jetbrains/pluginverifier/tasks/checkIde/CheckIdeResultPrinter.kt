package com.jetbrains.pluginverifier.tasks.checkIde

import com.jetbrains.pluginverifier.ide.IdeResourceUtil
import com.jetbrains.pluginverifier.output.OutputOptions
import com.jetbrains.pluginverifier.output.stream.WriterResultPrinter
import com.jetbrains.pluginverifier.output.teamcity.TeamCityLog
import com.jetbrains.pluginverifier.output.teamcity.TeamCityResultPrinter
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.results.VerificationResult
import com.jetbrains.pluginverifier.tasks.TaskResult
import com.jetbrains.pluginverifier.tasks.TaskResultPrinter
import java.io.File
import java.io.PrintWriter

class CheckIdeResultPrinter(val outputOptions: OutputOptions, val pluginRepository: PluginRepository) : TaskResultPrinter {

  override fun printResults(taskResult: TaskResult) {
    with(taskResult as CheckIdeResult) {
      if (outputOptions.needTeamCityLog) {
        printTcLog(outputOptions.teamCityGroupType, true, this)
      } else {
        printOnStdOut(this)
      }

      outputOptions.saveToHtmlFile(ideVersion, excludedPlugins, results)

      if (outputOptions.dumpBrokenPluginsFile != null) {
        val brokenPlugins = results
            .filter { it !is VerificationResult.OK && it !is VerificationResult.StructureWarnings }
            .map { it.plugin }
            .distinct()
        IdeResourceUtil.dumbBrokenPluginsList(File(outputOptions.dumpBrokenPluginsFile), brokenPlugins)
      }
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
        val totalProblemsNumber = results.flatMap {
          when (it) {
            is VerificationResult.Problems -> it.problems
            is VerificationResult.MissingDependencies -> it.problems //some problems might have been caused by missing dependencies
            is VerificationResult.InvalidPlugin -> setOf(Any())
            is VerificationResult.OK,
            is VerificationResult.StructureWarnings,
            is VerificationResult.NotFound,
            is VerificationResult.FailedToDownload -> emptySet()
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
      val resultPrinter = WriterResultPrinter(printWriter, outputOptions.missingDependencyIgnoring)
      resultPrinter.printResults(results)
      resultPrinter.printInvalidPluginFiles(invalidPluginFiles)
      printWriter.flush()
    }
  }
}