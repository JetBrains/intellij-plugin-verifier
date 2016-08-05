package com.jetbrains.pluginverifier.configurations

import com.google.common.collect.Multimap
import com.google.common.collect.Multimaps
import com.google.gson.annotations.SerializedName
import com.intellij.structure.domain.IdeVersion
import com.jetbrains.pluginverifier.dependencies.MissingPlugin
import com.jetbrains.pluginverifier.format.UpdateInfo
import com.jetbrains.pluginverifier.problems.Problem
import com.jetbrains.pluginverifier.report.CheckIdeReport

/**
 * @author Sergey Patrikeev
 */
data class CheckTrunkApiResults(@SerializedName("majorReport") val majorReport: CheckIdeReport,
                                @SerializedName("majorPlugins") val majorPlugins: BundledPlugins,
                                @SerializedName("currentReport") val currentReport: CheckIdeReport,
                                @SerializedName("currentPlugins") val currentPlugins: BundledPlugins) : Results {
}

data class BundledPlugins(@SerializedName("pluginIds") val pluginIds: List<String>,
                          @SerializedName("moduleIds") val moduleIds: List<String>)

data class CheckTrunkApiCompareResult(@SerializedName("curVersion") val currentVersion: IdeVersion,
                                      @SerializedName("majorVersion") val majorVersion: IdeVersion,
                                      @SerializedName("newProblems") val newProblems: Multimap<UpdateInfo, Problem>,
                                      @SerializedName("newMissingProblems") val newMissingProblems: Multimap<MissingPlugin, UpdateInfo>) {
  companion object {
    fun create(apiResults: CheckTrunkApiResults): CheckTrunkApiCompareResult {
      val oldProblems = apiResults.majorReport.pluginProblems.values().toSet()
      val newProblems = Multimaps.filterValues(apiResults.currentReport.pluginProblems, { it !in oldProblems })
      val newMissingProblems = Multimaps.filterEntries(apiResults.currentReport.missingPlugins, { it !in apiResults.majorReport.missingPlugins.entries() })
      return CheckTrunkApiCompareResult(apiResults.currentReport.ideVersion, apiResults.majorReport.ideVersion, newProblems, newMissingProblems)
    }
  }
}
