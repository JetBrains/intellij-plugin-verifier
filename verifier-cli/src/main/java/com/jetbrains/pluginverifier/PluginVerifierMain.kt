package com.jetbrains.pluginverifier

import com.jetbrains.pluginverifier.configurations.*
import com.jetbrains.pluginverifier.output.TeamCityLog
import com.jetbrains.pluginverifier.output.TeamCityPrinter
import com.jetbrains.pluginverifier.report.CheckIdeReport
import com.jetbrains.pluginverifier.utils.CmdOpts
import com.jetbrains.pluginverifier.utils.OptionsUtil
import com.jetbrains.pluginverifier.utils.PublicOpts
import com.sampullara.cli.Args
import org.slf4j.LoggerFactory
import java.io.File

object PluginVerifierMain {

  private val LOG = LoggerFactory.getLogger(PluginVerifierMain.javaClass)

  @JvmStatic fun main(args: Array<String>) {
    val opts = CmdOpts()
    var freeArgs = Args.parse(opts, args, false)

    if (freeArgs.isEmpty()) {
      System.err.println("""The command is not specified. Should be one of 'check-plugin' or 'check-ide'.
  Example: java -jar verifier.jar -r /usr/lib/jvm/java-8-oracle check-plugin /tmp/Kotlin /tmp/IU-162.2032.8 /tmp/IU-163.1024 /tmp/IU-163.7277
        OR java -jar verifier.jar -html-report report.html check-ide /tmp/IU-162.2032.8

  More examples on https://github.com/JetBrains/intellij-plugin-verifier/
""")
      Args.usage(System.err, PublicOpts())

      System.exit(1)
    }

    val command = freeArgs[0]
    freeArgs = freeArgs.drop(1)

    when (command) {
      "check-plugin" -> {
        CheckPluginParamsParser.parse(opts, freeArgs).use { params ->
          LOG.info("Check-Plugin arguments: $params")

          val results = CheckPluginConfiguration(params).execute()
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
      "check-ide" -> {
        CheckIdeParamsParser.parse(opts, freeArgs).use { params ->
          LOG.info("Check-Ide arguments: $params")

          val checkIdeResults = CheckIdeConfiguration(params).execute()

          if (opts.saveCheckIdeReport != null) {
            CheckIdeReport.createReport(checkIdeResults.ideVersion, checkIdeResults.results).saveToFile(File(opts.saveCheckIdeReport))
          }

          val printerOptions = OptionsUtil.parsePrinterOptions(opts)
          if (opts.needTeamCityLog) {
            checkIdeResults.printTcLog(TeamCityPrinter.GroupBy.parse(opts.group), true, printerOptions)
          } else {
            checkIdeResults.printOnStdOut(printerOptions)
          }

          if (opts.htmlReportFile != null) {
            checkIdeResults.saveToHtmlFile(File(opts.htmlReportFile), OptionsUtil.parsePrinterOptions(opts))
          }

          if (opts.dumpBrokenPluginsFile != null) {
            checkIdeResults.dumbBrokenPluginsList(File(opts.dumpBrokenPluginsFile))
          }
        }
      }
      "check-trunk-api" -> {
        CheckTrunkApiParamsParser.parse(opts, freeArgs).use { params ->
          LOG.info("Check-Trunk-API arguments: $params")

          val checkTrunkApiResults = CheckTrunkApiConfiguration(params).execute()

          if (opts.needTeamCityLog) {
            val compareResult = CheckTrunkApiCompareResult.create(checkTrunkApiResults)
            val vPrinter = TeamCityPrinter(TeamCityLog(System.out), TeamCityPrinter.GroupBy.parse(opts.group))
            vPrinter.printIdeCompareResult(compareResult)
          }
          if (opts.saveCheckIdeReport != null) {
            val file = File(opts.saveCheckIdeReport)
            checkTrunkApiResults.currentReport.saveToFile(file)
          }
        }
      }
      else -> {
        throw IllegalArgumentException("Unsupported command $command")
      }
    }
  }

}
