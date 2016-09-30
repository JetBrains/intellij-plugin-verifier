package com.jetbrains.pluginverifier.client.commands

import com.jetbrains.pluginverifier.client.BaseCmdOpts
import com.jetbrains.pluginverifier.configurations.CheckTrunkApiCompareResult
import com.jetbrains.pluginverifier.configurations.CheckTrunkApiResults
import com.jetbrains.pluginverifier.output.TeamCityLog
import com.jetbrains.pluginverifier.output.TeamCityVPrinter
import com.jetbrains.pluginverifier.utils.VOptionsUtil
import com.sampullara.cli.Args
import com.sampullara.cli.Argument
import org.jetbrains.plugins.verifier.service.api.CheckTrunkApi
import org.jetbrains.plugins.verifier.service.params.CheckTrunkApiRunnerParams
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

    val runnerParams = checkTrunkApiRunnerParams(opts, freeArgs)

    val results = CheckTrunkApi(opts.host, ideFile, runnerParams).execute()
    processResults(results, opts)
  }

  fun processResults(apiResults: CheckTrunkApiResults, opts: BaseCmdOpts) {
    if (opts.needTeamCityLog) {
      val compareResult = CheckTrunkApiCompareResult.create(apiResults)
      val vPrinter = TeamCityVPrinter(TeamCityLog(System.out), TeamCityVPrinter.GroupBy.parse(opts.group))
      vPrinter.printIdeCompareResult(compareResult)
    }
    if (opts.saveCheckIdeReport != null) {
      val file = File(opts.saveCheckIdeReport)
      apiResults.currentReport.saveToFile(file)
    }
  }

  private fun checkTrunkApiRunnerParams(opts: BaseCmdOpts, freeArgs: List<String>): CheckTrunkApiRunnerParams {
    val jdkVersion = BaseCmdOpts.parseJdkVersion(opts) ?: throw IllegalArgumentException("Specify the JDK version to check with")
    val vOptions = VOptionsUtil.parseOpts(opts)
    val apiOptions = CheckTrunkApiOptions()
    Args.parse(apiOptions, freeArgs.toTypedArray())

    val version = apiOptions.majorIdeVersion ?: throw IllegalArgumentException("You should specify the IDE version with which to compare check results")

    return CheckTrunkApiRunnerParams(jdkVersion, vOptions, version)
  }


}

private class CheckTrunkApiOptions : BaseCmdOpts() {
  @set:Argument("majorIdeVersion", alias = "miv", description = "The IDE version with which to compare API problems")
  var majorIdeVersion: String? = null
}