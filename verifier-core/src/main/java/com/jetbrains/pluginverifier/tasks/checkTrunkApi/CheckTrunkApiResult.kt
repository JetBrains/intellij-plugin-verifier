package com.jetbrains.pluginverifier.tasks.checkTrunkApi

import com.jetbrains.pluginverifier.output.teamcity.TeamCityLog
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.tasks.OutputOptions
import com.jetbrains.pluginverifier.tasks.TaskResult
import com.jetbrains.pluginverifier.tasks.checkIde.CheckIdeResult
import com.jetbrains.pluginverifier.tasks.checkTrunkApi.output.TrunkApiChangesOutput
import com.jetbrains.pluginverifier.tasks.checkTrunkApi.result.TrunkApiChanges
import java.io.File

/**
 * @author Sergey Patrikeev
 */
data class CheckTrunkApiResult(val trunkResult: CheckIdeResult,
                               val releaseResult: CheckIdeResult) : TaskResult {

  override fun printResults(outputOptions: OutputOptions, pluginRepository: PluginRepository) {
    if (outputOptions.needTeamCityLog) {
      val trunkApiChanges = TrunkApiChanges.create(releaseResult, trunkResult)
      TrunkApiChangesOutput(TeamCityLog(System.out), pluginRepository).printTrunkApiCompareResult(trunkApiChanges)
    }
    if (outputOptions.htmlReportFile != null) {
      val trunkHtmlReportFileName = outputOptions.htmlReportFile.name + "-trunk-${trunkResult.ideVersion}.html"
      saveIdeReportToHtmlFile(trunkResult, trunkHtmlReportFileName, outputOptions)

      val releaseHtmlReportFileName = outputOptions.htmlReportFile.name + "-release-${releaseResult.ideVersion}.html"
      saveIdeReportToHtmlFile(releaseResult, releaseHtmlReportFileName, outputOptions)
    }
  }

  private fun saveIdeReportToHtmlFile(checkIdeResults: CheckIdeResult, htmlFileName: String, outputOptions: OutputOptions) {
    checkIdeResults.saveToHtmlFile(File(htmlFileName), outputOptions)
  }

}
