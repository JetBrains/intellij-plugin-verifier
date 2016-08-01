package com.jetbrains.pluginverifier.configurations

import com.jetbrains.pluginverifier.api.VResult
import com.jetbrains.pluginverifier.api.VResults
import com.jetbrains.pluginverifier.output.TeamCityLog
import com.jetbrains.pluginverifier.output.TeamCityVPrinter

class CheckPluginResults(val vResults: VResults) : Results {

  fun printTcLog(groupBy: TeamCityVPrinter.GroupBy, setBuildStatus: Boolean) {
    val tcLog = TeamCityLog(System.out)
    val vPrinter = TeamCityVPrinter(tcLog, groupBy)
    vPrinter.printResults(vResults)
    if (setBuildStatus) {
      val totalProblemsNumber = vResults.results.flatMap {
        when (it) {
          is VResult.Nice -> setOf()
          is VResult.Problems -> it.problems.keySet()
          is VResult.BadPlugin -> setOf(Any())
          is VResult.NotFound -> setOf()
        }
      }.distinct().size
      if (totalProblemsNumber > 0) {
        tcLog.buildStatusFailure("$totalProblemsNumber problem${if (totalProblemsNumber > 0) "s" else ""} found")
      }
    }
  }

}
