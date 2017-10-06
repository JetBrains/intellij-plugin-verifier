package com.jetbrains.pluginverifier.tasks.checkIde

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.misc.create
import com.jetbrains.pluginverifier.output.html.HtmlResultPrinter
import com.jetbrains.pluginverifier.output.stream.WriterResultPrinter
import com.jetbrains.pluginverifier.output.teamcity.TeamCityLog
import com.jetbrains.pluginverifier.output.teamcity.TeamCityResultPrinter
import com.jetbrains.pluginverifier.parameters.ide.IdeResourceUtil
import com.jetbrains.pluginverifier.repository.PluginIdAndVersion
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.results.Result
import com.jetbrains.pluginverifier.results.Verdict
import com.jetbrains.pluginverifier.tasks.OutputOptions
import com.jetbrains.pluginverifier.tasks.TaskResult
import java.io.File
import java.io.PrintWriter

data class CheckIdeResult(val ideVersion: IdeVersion,
                          val results: List<Result>,
                          val excludedPlugins: List<PluginIdAndVersion>,
                          val noCompatibleUpdatesProblems: List<MissingCompatibleUpdate>) : TaskResult {

  override fun printResults(outputOptions: OutputOptions, pluginRepository: PluginRepository) {
    if (outputOptions.needTeamCityLog) {
      printTcLog(outputOptions.teamCityGroupType, true, pluginRepository, outputOptions)
    } else {
      printOnStdOut(outputOptions)
    }

    if (outputOptions.htmlReportFile != null) {
      saveToHtmlFile(outputOptions.htmlReportFile, outputOptions)
    }

    if (outputOptions.dumpBrokenPluginsFile != null) {
      val brokenPlugins = results
          .filter { it.verdict !is Verdict.OK && it.verdict !is Verdict.Warnings }
          .map { it.plugin }
          .map { PluginIdAndVersion(it.pluginId, it.version) }
          .distinct()
      IdeResourceUtil.dumbBrokenPluginsList(File(outputOptions.dumpBrokenPluginsFile), brokenPlugins)
    }
  }

  fun saveToHtmlFile(htmlFile: File, outputOptions: OutputOptions) {
    HtmlResultPrinter(listOf(ideVersion), { (pluginId, pluginVersion) ->
      PluginIdAndVersion(pluginId, pluginVersion) in excludedPlugins
    }, htmlFile.create(), outputOptions.missingDependencyIgnoring
    ).printResults(results)
  }

  private fun printTcLog(groupBy: TeamCityResultPrinter.GroupBy, setBuildStatus: Boolean, pluginRepository: PluginRepository, outputOptions: OutputOptions) {
    val tcLog = TeamCityLog(System.out)
    val resultPrinter = TeamCityResultPrinter(tcLog, groupBy, pluginRepository, outputOptions.missingDependencyIgnoring)
    resultPrinter.printResults(results)
    resultPrinter.printNoCompatibleUpdatesProblems(noCompatibleUpdatesProblems)
    if (setBuildStatus) {
      val totalProblemsNumber: Int = results.flatMap {
        when (it.verdict) {
          is Verdict.Problems -> it.verdict.problems //some problems might have been caused by missing dependencies
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

  private fun printOnStdOut(outputOptions: OutputOptions) {
    val printWriter = PrintWriter(System.out)
    WriterResultPrinter(printWriter, outputOptions.missingDependencyIgnoring).printResults(results)
    printWriter.flush()
  }

}
