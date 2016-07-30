package com.jetbrains.pluginverifier

import com.jetbrains.pluginverifier.configurations.CheckIdeConfiguration
import com.jetbrains.pluginverifier.configurations.CheckIdeParamsParser
import com.jetbrains.pluginverifier.configurations.CheckPluginConfiguration
import com.jetbrains.pluginverifier.configurations.CheckPluginParamsParser
import com.jetbrains.pluginverifier.output.TeamCityVPrinter
import com.jetbrains.pluginverifier.report.CheckIdeReport
import com.jetbrains.pluginverifier.utils.CmdOpts
import com.sampullara.cli.Args
import org.slf4j.LoggerFactory
import java.io.File

object PluginVerifierMain {

  private val LOG = LoggerFactory.getLogger(PluginVerifierMain.javaClass)

  @JvmStatic fun main(args: Array<String>) {
    val opts = CmdOpts()
    var freeArgs = Args.parse(opts, args)


    if (freeArgs.isEmpty()) {
      throw IllegalArgumentException("The command is not specified. Should be one of 'check-plugin' or 'check-ide'")
    }

    val command = freeArgs[0]
    freeArgs = freeArgs.drop(1)

    when (command) {
      "check-plugin" -> {
        val params = CheckPluginParamsParser.parse(opts, freeArgs)
        LOG.info("Check-Plugin arguments: $params")

        val results = CheckPluginConfiguration(params).execute()
        if (opts.needTeamCityLog) {
          results.printTcLog(TeamCityVPrinter.GroupBy.parse(opts.group), true)
        }
      }
      "check-ide" -> {
        val params = CheckIdeParamsParser.parse(opts, freeArgs)
        LOG.info("Check-Ide arguments: $params")

        val checkIdeResults = CheckIdeConfiguration(params).execute()

        if (opts.saveCheckIdeReport != null) {
          CheckIdeReport.createReport(checkIdeResults.ideVersion, checkIdeResults.vResults).saveToFile(File(opts.saveCheckIdeReport))
        }
        if (opts.needTeamCityLog) {
          checkIdeResults.printTcLog(TeamCityVPrinter.GroupBy.parse(opts.group), true)
        }
        if (opts.htmlReportFile != null) {
          checkIdeResults.saveToHtmlFile(File(opts.htmlReportFile))
        }
        if (opts.dumpBrokenPluginsFile != null) {
          checkIdeResults.dumbBrokenPluginsList(File(opts.dumpBrokenPluginsFile))
        }
      }
      else -> {
        throw IllegalArgumentException("Unsupported command $command")
      }
    }
  }

}
