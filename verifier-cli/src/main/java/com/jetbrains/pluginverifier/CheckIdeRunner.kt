package com.jetbrains.pluginverifier

import com.jetbrains.pluginverifier.configurations.CheckIdeConfiguration
import com.jetbrains.pluginverifier.configurations.CheckIdeParams
import com.jetbrains.pluginverifier.configurations.CheckIdeParamsParser
import com.jetbrains.pluginverifier.configurations.CheckIdeResults
import com.jetbrains.pluginverifier.output.TeamCityPrinter
import com.jetbrains.pluginverifier.utils.CmdOpts
import com.jetbrains.pluginverifier.utils.OptionsUtil
import java.io.File

class CheckIdeRunner : ConfigurationRunner<CheckIdeParams, CheckIdeParamsParser, CheckIdeResults, CheckIdeConfiguration>() {
  override val commandName: String = "check-ide"

  override fun getParamsParser(): CheckIdeParamsParser = CheckIdeParamsParser()

  override fun getConfiguration(): CheckIdeConfiguration = CheckIdeConfiguration()

  override fun printResults(results: CheckIdeResults, opts: CmdOpts) {
    val printerOptions = OptionsUtil.parsePrinterOptions(opts)
    if (opts.needTeamCityLog) {
      results.printTcLog(TeamCityPrinter.GroupBy.parse(opts.group), true, printerOptions)
    } else {
      results.printOnStdOut(printerOptions)
    }

    if (opts.htmlReportFile != null) {
      results.saveToHtmlFile(File(opts.htmlReportFile), OptionsUtil.parsePrinterOptions(opts))
    }

    if (opts.dumpBrokenPluginsFile != null) {
      results.dumbBrokenPluginsList(File(opts.dumpBrokenPluginsFile))
    }
  }

}