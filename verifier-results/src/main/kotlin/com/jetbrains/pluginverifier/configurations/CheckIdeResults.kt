package com.jetbrains.pluginverifier.configurations

import com.google.gson.annotations.SerializedName
import com.intellij.structure.ide.IdeVersion
import com.jetbrains.pluginverifier.api.Result
import com.jetbrains.pluginverifier.api.Verdict
import com.jetbrains.pluginverifier.misc.VersionComparatorUtil
import com.jetbrains.pluginverifier.misc.create
import com.jetbrains.pluginverifier.output.*
import com.jetbrains.pluginverifier.utils.ParametersListUtil
import java.io.File
import java.io.PrintWriter

data class CheckIdeResults(@SerializedName("ideVersion") val ideVersion: IdeVersion,
                           @SerializedName("results") val results: List<Result>,
                           @SerializedName("excludedPlugins") val excludedPlugins: List<PluginIdAndVersion>,
                           @SerializedName("noUpdatesProblems") val noCompatibleUpdatesProblems: List<MissingCompatibleUpdate>) : ConfigurationResults {

  fun dumbBrokenPluginsList(dumpBrokenPluginsFile: File) {
    PrintWriter(dumpBrokenPluginsFile.create()).use { out ->
      out.println("// This file contains list of broken plugins.\n" +
          "// Each line contains plugin ID and list of versions that are broken.\n" +
          "// If plugin name or version contains a space you can quote it like in command line.\n")

      val brokenPlugins = results.filterNot { it.verdict is Verdict.OK }.map { it.plugin }.map { it.pluginId to it.version }.distinct()
      brokenPlugins.groupBy { it.first }.forEach {
        out.print(ParametersListUtil.join(listOf(it.key)))
        out.print("    ")
        out.println(ParametersListUtil.join(it.value.map { it.second }.sortedWith(VersionComparatorUtil.COMPARATOR)))
      }
    }
  }

  fun saveToHtmlFile(htmlFile: File, printerOptions: PrinterOptions) {
    HtmlPrinter(ideVersion, { (pluginId, pluginVersion) -> PluginIdAndVersion(pluginId, pluginVersion) in excludedPlugins }, htmlFile.create()).printResults(results, printerOptions)
  }

  fun printTcLog(groupBy: TeamCityPrinter.GroupBy, setBuildStatus: Boolean, vPrinterOptions: PrinterOptions) {
    val tcLog = TeamCityLog(System.out)
    val vPrinter = TeamCityPrinter(tcLog, groupBy)
    vPrinter.printResults(results, vPrinterOptions)
    vPrinter.printNoCompatibleUpdatesProblems(noCompatibleUpdatesProblems)
    if (setBuildStatus) {
      val totalProblemsNumber: Int = results.flatMap {
        when (it.verdict) {
          is Verdict.OK -> emptySet()
          is Verdict.Warnings -> emptySet()
          is Verdict.Problems -> it.verdict.problems //some problems might be caused by missing dependencies
          is Verdict.Bad -> setOf(Any())
          is Verdict.NotFound -> emptySet()
          is Verdict.MissingDependencies -> emptySet()
        }
      }.distinct().size
      if (totalProblemsNumber > 0) {
        tcLog.buildStatusFailure("IDE $ideVersion has $totalProblemsNumber problem${if (totalProblemsNumber > 1) "s" else ""}")
      } else {
        tcLog.buildStatusSuccess("IDE $ideVersion doesn't have broken API problems")
      }

    }
  }

  fun printOnStdOut(vPrinterOptions: PrinterOptions) {
    val printWriter = PrintWriter(System.out)
    WriterPrinter(printWriter).printResults(results, vPrinterOptions)
    printWriter.flush()
  }

}
