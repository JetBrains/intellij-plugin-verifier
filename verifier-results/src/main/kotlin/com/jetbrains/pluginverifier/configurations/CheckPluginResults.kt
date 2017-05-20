package com.jetbrains.pluginverifier.configurations

import com.jetbrains.pluginverifier.api.Result
import com.jetbrains.pluginverifier.api.Verdict
import com.jetbrains.pluginverifier.misc.create
import com.jetbrains.pluginverifier.output.*
import java.io.File
import java.io.PrintWriter

data class CheckPluginResults(val results: List<Result>) : ConfigurationResults {

  fun printTcLog(groupBy: TeamCityPrinter.GroupBy, setBuildStatus: Boolean, vPrinterOptions: PrinterOptions) {
    val tcLog = TeamCityLog(System.out)
    val vPrinter = TeamCityPrinter(tcLog, groupBy)
    vPrinter.printResults(results, vPrinterOptions)
    if (setBuildStatus) {
      val totalProblemsNumber = results.flatMap {
        when (it.verdict) {
          is Verdict.OK -> emptySet()
          is Verdict.Warnings -> emptySet()
          is Verdict.Problems -> it.verdict.problems
          is Verdict.MissingDependencies -> it.verdict.problems  //some problems might be caused by missing dependencies
          is Verdict.Bad -> setOf(Any())
          is Verdict.NotFound -> emptySet()
        }
      }.distinct().size
      if (totalProblemsNumber > 0) {
        tcLog.buildStatusFailure("$totalProblemsNumber problem${if (totalProblemsNumber > 0) "s" else ""} found")
      }
    }
  }

  fun printOnStdout(vPrinterOptions: PrinterOptions) {
    val printWriter = PrintWriter(System.out)
    WriterPrinter(printWriter).printResults(results, vPrinterOptions)
    printWriter.flush()
  }

  fun printToHtml(file: File, vPrinterOptions: PrinterOptions) {
    val ideVersion = results[0].ideVersion
    if (results.size > 1) {
      System.err.println("Warning! HTML report for multiple IDE builds is not supported yet! We are working on it just now...\n" +
          "Only the result for $ideVersion is saved to file $file")
    }
    HtmlPrinter(ideVersion, { false }, file.create()).printResults(results, vPrinterOptions)
  }

}
