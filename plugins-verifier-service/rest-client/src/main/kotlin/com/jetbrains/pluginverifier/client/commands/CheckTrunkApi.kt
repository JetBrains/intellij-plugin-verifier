package com.jetbrains.pluginverifier.client.commands

import com.jetbrains.pluginverifier.client.BaseCmdOpts
import com.jetbrains.pluginverifier.configurations.CheckIdeCompareResult
import com.jetbrains.pluginverifier.output.HtmlVPrinter
import com.jetbrains.pluginverifier.output.TeamCityLog
import com.jetbrains.pluginverifier.output.TeamCityVPrinter
import com.jetbrains.pluginverifier.utils.VOptionsUtil
import org.jetbrains.plugins.verifier.service.api.CheckTrunkApi
import org.jetbrains.plugins.verifier.service.params.CheckTrunkApiRunnerParams
import org.jetbrains.plugins.verifier.service.results.CheckTrunkApiResults
import java.io.File

/**
 * @author Sergey Patrikeev
 */
class CheckTrunkApiCommand : Command {
  override fun name(): String = "check-trunk-api"

  override fun execute(opts: BaseCmdOpts, freeArgs: List<String>) {
    if (freeArgs.isEmpty()) {
      throw IllegalArgumentException("You have to specify the IDE to check")
    }
    val ideFile = File(freeArgs[0])
    if (!ideFile.exists()) {
      throw IllegalArgumentException("IDE $ideFile doesn't exist")
    }

    val runnerParams = checkTrunkApiRunnerParams(opts)

    val results = CheckTrunkApi(opts.host, ideFile, runnerParams).execute()
    processResults(results, opts)
  }

  fun processResults(apiResults: CheckTrunkApiResults, opts: BaseCmdOpts) {
    if (opts.needTeamCityLog) {
      val vPrinter = TeamCityVPrinter(TeamCityLog(System.out), TeamCityVPrinter.GroupBy.parse(opts.group))
      val compareResult = CheckIdeCompareResult.compareWithPreviousReports(listOf(apiResults.majorReport), apiResults.currentReport)
      vPrinter.printIdeCompareResult(compareResult)
    }
    if (opts.saveCheckIdeReport != null) {
      val file = File(opts.saveCheckIdeReport)
      apiResults.currentReport.saveToFile(file)
    }
    if (opts.htmlReportFile != null) {
      HtmlVPrinter(apiResults.currentReport.ideVersion, { false }, File(opts.htmlReportFile))
    }
  }

  private fun checkTrunkApiRunnerParams(opts: BaseCmdOpts): CheckTrunkApiRunnerParams {
    val jdkVersion = BaseCmdOpts.parseJdkVersion(opts) ?: throw IllegalArgumentException("Specify the JDK version to check with")
    val vOptions = VOptionsUtil.parseOpts(opts)

    return CheckTrunkApiRunnerParams(jdkVersion, vOptions)
  }

}