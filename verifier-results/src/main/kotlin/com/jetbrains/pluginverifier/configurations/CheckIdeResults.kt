package com.jetbrains.pluginverifier.configurations

import com.google.common.collect.Multimap
import com.google.gson.annotations.SerializedName
import com.intellij.structure.domain.IdeVersion
import com.jetbrains.pluginverifier.api.Result
import com.jetbrains.pluginverifier.api.Verdict
import com.jetbrains.pluginverifier.misc.VersionComparatorUtil
import com.jetbrains.pluginverifier.output.*
import com.jetbrains.pluginverifier.utils.ParametersListUtil
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.PrintWriter

fun File.create(): File {
  if (this.parentFile != null) {
    FileUtils.forceMkdir(this.parentFile)
  }
  this.createNewFile()
  return this
}

data class MissingCompatibleUpdate(val pluginId: String, val ideVersion: IdeVersion, val details: String) {
  override fun toString(): String = "For $pluginId there are no updates compatible with $ideVersion in the Plugin Repository${if (details.isNullOrEmpty()) "" else " ($details)"}"
}

data class CheckIdeResults(@SerializedName("ideVersion") val ideVersion: IdeVersion,
                           @SerializedName("results") val results: List<Result>,
                           @SerializedName("excludedPlugins") val excludedPlugins: Multimap<String, String>,
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

  fun saveToHtmlFile(htmlFile: File, vPrinterOptions: PrinterOptions) {
    HtmlVPrinter(ideVersion, { x -> excludedPlugins.containsEntry(x.first, x.second) }, htmlFile.create()).printResults(results, vPrinterOptions)
  }

  fun printTcLog(groupBy: TeamCityVPrinter.GroupBy, setBuildStatus: Boolean, vPrinterOptions: PrinterOptions) {
    val tcLog = TeamCityLog(System.out)
    val vPrinter = TeamCityVPrinter(tcLog, groupBy)
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
    WriterVPrinter(printWriter).printResults(results, vPrinterOptions)
    printWriter.flush()
  }

}
