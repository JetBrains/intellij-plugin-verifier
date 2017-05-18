package com.jetbrains.pluginverifier.configurations

import com.jetbrains.pluginverifier.api.Verdict
import com.jetbrains.pluginverifier.api.VerificationResult
import com.jetbrains.pluginverifier.misc.create
import com.jetbrains.pluginverifier.output.*
import java.io.File
import java.io.PrintWriter

data class CheckPluginResults(val results: List<VerificationResult>) : ConfigurationResults {

  fun printTcLog(groupBy: TeamCityVPrinter.GroupBy, setBuildStatus: Boolean, vPrinterOptions: PrinterOptions) {
    val tcLog = TeamCityLog(System.out)
    val vPrinter = TeamCityVPrinter(tcLog, groupBy)
    vPrinter.printResults(results, vPrinterOptions)
    if (setBuildStatus) {
      val totalProblemsNumber = results.filterIsInstance<VerificationResult.Verified>().flatMap {
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
    WriterVPrinter(printWriter).printResults(results, vPrinterOptions)
    printWriter.flush()
  }

  fun printToHtml(file: File, vPrinterOptions: PrinterOptions) {
    val ideVersion = results[0].ideDescriptor.ideVersion
    if (results.size > 1) {
      System.err.println("Warning! HTML report for multiple IDE builds is not supported yet! We are working on it just now...\n" +
          "Only the result for $ideVersion is saved to file $file")
    }
    HtmlPrinter(ideVersion, { false }, file.create()).printResults(results, vPrinterOptions)
  }

}
