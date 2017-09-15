package com.jetbrains.pluginverifier.tasks

import com.jetbrains.pluginverifier.output.PrinterOptions
import com.jetbrains.pluginverifier.output.TeamCityLog
import com.jetbrains.pluginverifier.output.TrunkApiChangesOutput
import com.jetbrains.pluginverifier.repository.PluginRepository
import java.io.File

/**
 * @author Sergey Patrikeev
 */
data class CheckTrunkApiResult(val trunkResult: CheckIdeResult,
                               val releaseResult: CheckIdeResult) : TaskResult {

  override fun printResults(printerOptions: PrinterOptions, pluginRepository: PluginRepository) {
    if (printerOptions.needTeamCityLog) {
      val trunkApiChanges = TrunkApiChanges.create(releaseResult, trunkResult)
      val printer = TrunkApiChangesOutput(TeamCityLog(System.out), pluginRepository)
      printer.printTrunkApiCompareResult(trunkApiChanges)
    }
    if (printerOptions.htmlReportFile != null) {
      val trunkHtmlReportFileName = printerOptions.htmlReportFile + "-trunk-${trunkResult.ideVersion}.html"
      saveIdeReportToHtmlFile(trunkResult, trunkHtmlReportFileName, printerOptions)

      val releaseHtmlReportFileName = printerOptions.htmlReportFile + "-release-${releaseResult.ideVersion}.html"
      saveIdeReportToHtmlFile(releaseResult, releaseHtmlReportFileName, printerOptions)
    }
  }

  private fun saveIdeReportToHtmlFile(checkIdeResults: CheckIdeResult, htmlFileName: String, printerOptions: PrinterOptions) {
    checkIdeResults.saveToHtmlFile(File(htmlFileName), printerOptions)
  }

}
