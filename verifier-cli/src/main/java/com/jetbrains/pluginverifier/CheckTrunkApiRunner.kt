package com.jetbrains.pluginverifier

import com.jetbrains.pluginverifier.configurations.*
import com.jetbrains.pluginverifier.output.PrinterOptions
import com.jetbrains.pluginverifier.output.TeamCityLog
import com.jetbrains.pluginverifier.output.TeamCityPrinter
import com.jetbrains.pluginverifier.utils.CmdOpts
import java.io.File

class CheckTrunkApiRunner : ConfigurationRunner<CheckTrunkApiParams, CheckTrunkApiParamsParser, CheckTrunkApiResults, CheckTrunkApiConfiguration>() {
  override val commandName: String = "check-trunk-api"

  override fun getParamsParser(): CheckTrunkApiParamsParser = CheckTrunkApiParamsParser()

  override fun getConfiguration(): CheckTrunkApiConfiguration = CheckTrunkApiConfiguration()

  override fun printResults(results: CheckTrunkApiResults, opts: CmdOpts) {
    if (opts.needTeamCityLog) {
      val compareResult = CheckTrunkApiCompareResult.create(results)
      val printer = TeamCityPrinter(TeamCityLog(System.out), TeamCityPrinter.GroupBy.parse(opts.group))
      printer.printTrunkApiCompareResult(compareResult)
    }
    if (opts.htmlReportFile != null) {
      val trunkHtmlReportFileName = opts.htmlReportFile + "-trunk-${results.trunkResults.ideVersion}.html"
      saveIdeReportToHtmlFile(results.trunkResults, trunkHtmlReportFileName)

      val releaseHtmlReportFileName = opts.htmlReportFile + "-release-${results.releaseResults.ideVersion}.html"
      saveIdeReportToHtmlFile(results.releaseResults, releaseHtmlReportFileName)
    }
  }

  private fun saveIdeReportToHtmlFile(checkIdeResults: CheckIdeResults, htmlFileName: String) {
    checkIdeResults.saveToHtmlFile(File(htmlFileName), PrinterOptions(false, emptyList()))
  }

}