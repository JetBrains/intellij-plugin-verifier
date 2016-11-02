package com.jetbrains.pluginverifier.configurations

import com.google.gson.annotations.SerializedName
import com.jetbrains.pluginverifier.api.VResult
import com.jetbrains.pluginverifier.api.VResults
import com.jetbrains.pluginverifier.output.TeamCityLog
import com.jetbrains.pluginverifier.output.TeamCityVPrinter
import com.jetbrains.pluginverifier.output.VPrinterOptions

data class CheckPluginResults(@SerializedName("results") val vResults: VResults) : Results {

  fun printTcLog(groupBy: TeamCityVPrinter.GroupBy, setBuildStatus: Boolean, vPrinterOptions: VPrinterOptions) {
    val tcLog = TeamCityLog(System.out)
    val vPrinter = TeamCityVPrinter(tcLog, groupBy)
    vPrinter.printResults(vResults, vPrinterOptions)
    if (setBuildStatus) {
      val totalProblemsNumber = vResults.results.flatMap {
        when (it) {
          is VResult.Nice -> setOf()
          is VResult.Problems -> it.problems.keySet() //some problems might be caused by missing dependencies
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
