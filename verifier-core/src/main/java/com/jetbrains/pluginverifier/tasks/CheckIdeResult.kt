package com.jetbrains.pluginverifier.tasks

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.misc.create
import com.jetbrains.pluginverifier.output.*
import com.jetbrains.pluginverifier.parameters.ide.IdeResourceUtil
import com.jetbrains.pluginverifier.repository.PluginIdAndVersion
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.results.Result
import com.jetbrains.pluginverifier.results.Verdict
import java.io.File
import java.io.PrintWriter

data class CheckIdeResult(val ideVersion: IdeVersion,
                          val results: List<Result>,
                          val excludedPlugins: List<PluginIdAndVersion>,
                          val noCompatibleUpdatesProblems: List<MissingCompatibleUpdate>) : TaskResult {

  override fun printResults(printerOptions: PrinterOptions, pluginRepository: PluginRepository) {
    if (printerOptions.needTeamCityLog) {
      printTcLog(TeamCityPrinter.GroupBy.parse(printerOptions.teamCityGroupType), true, printerOptions, pluginRepository)
    } else {
      printOnStdOut(printerOptions)
    }

    if (printerOptions.htmlReportFile != null) {
      saveToHtmlFile(File(printerOptions.htmlReportFile), printerOptions)
    }

    if (printerOptions.dumpBrokenPluginsFile != null) {
      val brokenPlugins = results
          .filter { it.verdict !is Verdict.OK && it.verdict !is Verdict.Warnings }
          .map { it.plugin }
          .map { PluginIdAndVersion(it.pluginId, it.version) }
          .distinct()
      IdeResourceUtil.dumbBrokenPluginsList(File(printerOptions.dumpBrokenPluginsFile), brokenPlugins)
    }
  }

  fun saveToHtmlFile(htmlFile: File, printerOptions: PrinterOptions) {
    HtmlPrinter(listOf(ideVersion), { (pluginId, pluginVersion) -> PluginIdAndVersion(pluginId, pluginVersion) in excludedPlugins }, htmlFile.create()).printResults(results, printerOptions)
  }

  private fun printTcLog(groupBy: TeamCityPrinter.GroupBy, setBuildStatus: Boolean, vPrinterOptions: PrinterOptions, pluginRepository: PluginRepository) {
    val tcLog = TeamCityLog(System.out)
    val vPrinter = TeamCityPrinter(tcLog, groupBy, pluginRepository)
    vPrinter.printResults(results, vPrinterOptions)
    vPrinter.printNoCompatibleUpdatesProblems(noCompatibleUpdatesProblems)
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

  fun printOnStdOut(vPrinterOptions: PrinterOptions) {
    val printWriter = PrintWriter(System.out)
    WriterPrinter(printWriter).printResults(results, vPrinterOptions)
    printWriter.flush()
  }

}
