package com.jetbrains.pluginverifier.configurations

import com.google.common.collect.Multimap
import com.google.gson.annotations.SerializedName
import com.intellij.structure.domain.IdeVersion
import com.jetbrains.pluginverifier.api.VResult
import com.jetbrains.pluginverifier.api.VResults
import com.jetbrains.pluginverifier.misc.VersionComparatorUtil
import com.jetbrains.pluginverifier.output.HtmlVPrinter
import com.jetbrains.pluginverifier.output.TeamCityLog
import com.jetbrains.pluginverifier.output.TeamCityVPrinter
import com.jetbrains.pluginverifier.utils.ParametersListUtil
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.PrintWriter

private fun File.create(): File {
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
                           @SerializedName("results") val vResults: VResults,
                           @SerializedName("excludedPlugins") val excludedPlugins: Multimap<String, String>,
                           @SerializedName("noUpdatesProblems") val noCompatibleUpdatesProblems: List<MissingCompatibleUpdate>) : Results {

  fun dumbBrokenPluginsList(dumpBrokenPluginsFile: File) {
    PrintWriter(dumpBrokenPluginsFile.create()).use { out ->
      out.println("// This file contains list of broken plugins.\n" +
          "// Each line contains plugin ID and list of versions that are broken.\n" +
          "// If plugin name or version contains a space you can quote it like in command line.\n")

      val brokenPlugins = vResults.results.filterNot { it is VResult.Nice }.map { it.pluginDescriptor }.map { it.pluginId to it.version }.distinct()
      brokenPlugins.groupBy { it.first }.forEach {
        out.print(ParametersListUtil.join(listOf(it.key)))
        out.print("    ")
        out.println(ParametersListUtil.join(it.value.map { it.second }.sortedWith(VersionComparatorUtil.COMPARATOR)))
      }
    }
  }

  fun saveToHtmlFile(htmlFile: File) {
    HtmlVPrinter(ideVersion, { x -> excludedPlugins.containsEntry(x.first, x.second) }, htmlFile.create()).printResults(vResults)
  }

  fun printTcLog(groupBy: TeamCityVPrinter.GroupBy, setBuildStatus: Boolean) {
    val tcLog = TeamCityLog(System.out)
    val vPrinter = TeamCityVPrinter(tcLog, groupBy)
    vPrinter.printResults(vResults)
    vPrinter.printNoCompatibleUpdatesProblems(noCompatibleUpdatesProblems)
    if (setBuildStatus) {
      val totalProblemsNumber: Int = vResults.results.flatMap {
        when (it) {
          is VResult.Nice -> setOf()
          is VResult.Problems -> it.problems.keySet() //some problems might be caused by missing dependencies
          is VResult.BadPlugin -> setOf(Any())
          is VResult.NotFound -> setOf()
        }
      }.distinct().size
      if (totalProblemsNumber > 0) {
        tcLog.buildStatusFailure("IDE $ideVersion has $totalProblemsNumber problem${if (totalProblemsNumber > 1) "s" else ""}")
      } else {
        tcLog.buildStatusSuccess("IDE $ideVersion doesn't have broken API problems")
      }

    }
  }

}
