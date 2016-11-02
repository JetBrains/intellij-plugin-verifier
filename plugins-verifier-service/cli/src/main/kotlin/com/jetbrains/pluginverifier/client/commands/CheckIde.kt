package com.jetbrains.pluginverifier.client.commands

import com.jetbrains.pluginverifier.client.BaseCmdOpts
import com.jetbrains.pluginverifier.configurations.CheckIdeParamsParser
import com.jetbrains.pluginverifier.configurations.CheckIdeResults
import com.jetbrains.pluginverifier.output.TeamCityVPrinter
import com.jetbrains.pluginverifier.report.CheckIdeReport
import com.jetbrains.pluginverifier.utils.CmdUtil
import com.jetbrains.pluginverifier.utils.VOptionsUtil
import org.jetbrains.plugins.verifier.service.api.CheckIde
import org.jetbrains.plugins.verifier.service.params.CheckIdeRunnerParams
import org.jetbrains.plugins.verifier.service.params.JdkVersion
import java.io.File

/**
 * @author Sergey Patrikeev
 */
class CheckIdeCommand : Command {
  override fun name(): String = "check-ide"

  override fun execute(opts: BaseCmdOpts, freeArgs: List<String>) {
    if (freeArgs.isEmpty()) {
      throw IllegalArgumentException("You should specify the IDE to check")
    }
    val ideFile = File(freeArgs[0])
    if (!ideFile.exists()) {
      throw IllegalArgumentException("IDE $ideFile doesn't exist")
    }

    val runnerParams = checkIdeRunnerParams(opts)
    val results = CheckIde(opts.host, ideFile, runnerParams).execute()

    processResults(results, opts)
  }

  fun processResults(checkIdeResults: CheckIdeResults, opts: BaseCmdOpts) {
    val report = CheckIdeReport.createReport(checkIdeResults.ideVersion, checkIdeResults.vResults)

    if (opts.saveCheckIdeReport != null) {
      val file = File(opts.saveCheckIdeReport)
      report.saveToFile(file)
    }
    if (opts.needTeamCityLog) {
      checkIdeResults.printTcLog(TeamCityVPrinter.GroupBy.parse(opts.group), true, VOptionsUtil.parseVPrinterOptions(opts))
    }
    if (opts.htmlReportFile != null) {
      checkIdeResults.saveToHtmlFile(File(opts.htmlReportFile), VOptionsUtil.parseVPrinterOptions(opts))
    }
    if (opts.dumpBrokenPluginsFile != null) {
      checkIdeResults.dumbBrokenPluginsList(File(opts.dumpBrokenPluginsFile))
    }
  }


  private fun checkIdeRunnerParams(opts: BaseCmdOpts): CheckIdeRunnerParams {
    val jdkVersion: JdkVersion = BaseCmdOpts.parseJdkVersion(opts) ?: throw IllegalArgumentException("Specify the JDK version to check with")

    val actualIdeVersion = CmdUtil.takeVersionFromCmd(opts)
    val vOptions = VOptionsUtil.parseOpts(opts)

    val (checkAllBuilds, checkLastBuilds) = CheckIdeParamsParser.parsePluginToCheckList(opts)
    val excludedPlugins = CheckIdeParamsParser.parseExcludedPlugins(opts)

    return CheckIdeRunnerParams(jdkVersion, vOptions, checkAllBuilds, checkLastBuilds, excludedPlugins, checkAllBuilds, actualIdeVersion)
  }


}