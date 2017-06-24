package com.jetbrains.pluginverifier.configurations

import com.google.gson.annotations.SerializedName
import com.jetbrains.pluginverifier.output.PrinterOptions
import com.jetbrains.pluginverifier.output.TeamCityLog
import com.jetbrains.pluginverifier.output.TeamCityPrinter
import java.io.File

/**
 * @author Sergey Patrikeev
 */
data class CheckTrunkApiResult(@SerializedName("trunkResults") val trunkResults: CheckIdeResult,
                               @SerializedName("releaseResults") val releaseResults: CheckIdeResult) : TaskResult {

  override fun printResults(printerOptions: PrinterOptions) {
    if (printerOptions.needTeamCityLog) {
      val compareResult = CheckTrunkApiCompareResult.create(this)
      val printer = TeamCityPrinter(TeamCityLog(System.out), TeamCityPrinter.GroupBy.parse(printerOptions.teamCityGroupType))
      printer.printTrunkApiCompareResult(compareResult)
    }
    if (printerOptions.htmlReportFile != null) {
      val trunkHtmlReportFileName = printerOptions.htmlReportFile + "-trunk-${trunkResults.ideVersion}.html"
      saveIdeReportToHtmlFile(trunkResults, trunkHtmlReportFileName, printerOptions)

      val releaseHtmlReportFileName = printerOptions.htmlReportFile + "-release-${releaseResults.ideVersion}.html"
      saveIdeReportToHtmlFile(releaseResults, releaseHtmlReportFileName, printerOptions)
    }
  }

  private fun saveIdeReportToHtmlFile(checkIdeResults: CheckIdeResult, htmlFileName: String, printerOptions: PrinterOptions) {
    checkIdeResults.saveToHtmlFile(File(htmlFileName), printerOptions)
  }

}
