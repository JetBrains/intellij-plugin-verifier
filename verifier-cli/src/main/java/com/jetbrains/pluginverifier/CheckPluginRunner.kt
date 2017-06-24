package com.jetbrains.pluginverifier

import com.jetbrains.pluginverifier.configurations.CheckPluginConfiguration
import com.jetbrains.pluginverifier.configurations.CheckPluginParams
import com.jetbrains.pluginverifier.configurations.CheckPluginParamsParser
import com.jetbrains.pluginverifier.configurations.CheckPluginResults
import com.jetbrains.pluginverifier.output.TeamCityPrinter
import com.jetbrains.pluginverifier.utils.CmdOpts
import com.jetbrains.pluginverifier.utils.OptionsUtil
import java.io.File

class CheckPluginRunner : ConfigurationRunner<CheckPluginParams, CheckPluginParamsParser, CheckPluginResults, CheckPluginConfiguration>() {
  override val commandName: String = "check-plugin"

  override fun getParamsParser(): CheckPluginParamsParser = CheckPluginParamsParser()

  override fun getConfiguration(): CheckPluginConfiguration = CheckPluginConfiguration()

  override fun printResults(results: CheckPluginResults, opts: CmdOpts) {
    val printerOptions = OptionsUtil.parsePrinterOptions(opts)
    if (opts.needTeamCityLog) {
      results.printTcLog(TeamCityPrinter.GroupBy.parse(opts.group), true, printerOptions)
    } else {
      results.printOnStdout(printerOptions)
    }

    if (opts.htmlReportFile != null) {
      results.printToHtml(File(opts.htmlReportFile), printerOptions)
    }
  }

}