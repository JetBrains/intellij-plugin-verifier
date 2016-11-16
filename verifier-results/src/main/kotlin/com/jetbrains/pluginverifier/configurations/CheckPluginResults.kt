package com.jetbrains.pluginverifier.configurations

import com.google.gson.annotations.SerializedName
import com.jetbrains.pluginverifier.api.VResult
import com.jetbrains.pluginverifier.api.VResults
import com.jetbrains.pluginverifier.output.*
import java.io.File
import java.io.PrintWriter

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

  fun printOnStdout(vPrinterOptions: VPrinterOptions) {
    val printWriter = PrintWriter(System.out)
    WriterVPrinter(printWriter).printResults(vResults, vPrinterOptions)
    printWriter.flush()
  }

  fun printToHtml(file: File, vPrinterOptions: VPrinterOptions) {
    val ideVersion = vResults.results[0].ideDescriptor.ideVersion
    if (vResults.results.size > 1) {
      System.err.println("Warning! HTML report for multiple IDE builds is not supported yet! We are working on it just now...\n" +
          "Only the result for $ideVersion is saved to file $file")
    }
    HtmlVPrinter(ideVersion, { false }, file.create()).printResults(vResults, vPrinterOptions)
  }

}
