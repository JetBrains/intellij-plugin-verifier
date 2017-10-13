package com.jetbrains.pluginverifier.tasks.checkTrunkApi

import com.jetbrains.pluginverifier.output.OutputOptions
import com.jetbrains.pluginverifier.output.teamcity.TeamCityLog
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.tasks.TaskResult
import com.jetbrains.pluginverifier.tasks.TaskResultPrinter
import com.jetbrains.pluginverifier.tasks.checkIde.CheckIdeResultPrinter
import com.jetbrains.pluginverifier.tasks.checkTrunkApi.output.TrunkApiChangesOutput
import com.jetbrains.pluginverifier.tasks.checkTrunkApi.result.TrunkApiChanges
import java.io.File

/**
 * @author Sergey Patrikeev
 */
class CheckTrunkApiResultPrinter(private val outputOptions: OutputOptions,
                                 private val pluginRepository: PluginRepository) : TaskResultPrinter {

  override fun printResults(taskResult: TaskResult) {
    with(taskResult as CheckTrunkApiResult) {
      if (outputOptions.needTeamCityLog) {
        val trunkApiChanges = TrunkApiChanges.create(releaseResult, trunkResult)
        TrunkApiChangesOutput(TeamCityLog(System.out), pluginRepository).printTrunkApiCompareResult(trunkApiChanges)
      }
      if (outputOptions.htmlReportFile != null) {
        val ideResultPrinter = CheckIdeResultPrinter(outputOptions, pluginRepository)

        val trunkHtmlReportFileName = outputOptions.htmlReportFile.name + "-trunk-${trunkResult.ideVersion}.html"
        ideResultPrinter.saveToHtmlFile(File(trunkHtmlReportFileName), trunkResult)

        val releaseHtmlReportFileName = outputOptions.htmlReportFile.name + "-release-${releaseResult.ideVersion}.html"
        ideResultPrinter.saveToHtmlFile(File(releaseHtmlReportFileName), releaseResult)
      }
    }
  }

}