package com.jetbrains.pluginverifier.configurations

import com.google.common.collect.Multimap
import com.google.common.collect.Multimaps
import com.google.gson.annotations.SerializedName
import com.intellij.structure.domain.IdeVersion
import com.jetbrains.pluginverifier.format.UpdateInfo
import com.jetbrains.pluginverifier.problems.MissingDependencyProblem
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

data class CheckTrunkApiCompareResult(val currentVersion: IdeVersion,
                                      val majorVersion: IdeVersion,
                                      val newProblems: Multimap<UpdateInfo, Problem>,
                                      val newMissingProblems: List<MissingDependencyProblem>) {
  companion object {
    fun create(apiResults: CheckTrunkApiResults): CheckTrunkApiCompareResult {
      val oldProblems = apiResults.majorReport.pluginProblems.values().toSet()
      val oldMissingIds = oldProblems.filterIsInstance<MissingDependencyProblem>().map { it.missingId }
      val newProblems = Multimaps.filterValues(apiResults.currentReport.pluginProblems, { it !in oldProblems && it !is MissingDependencyProblem })
      val newMissingProblems = apiResults.currentReport.pluginProblems.values().distinct()
          .filterIsInstance<MissingDependencyProblem>()
          .filterNot { it.missingId in oldMissingIds }
          .filter { it.missingId in apiResults.majorPlugins.pluginIds || it.missingId in apiResults.majorPlugins.moduleIds }

      return CheckTrunkApiCompareResult(apiResults.currentReport.ideVersion, apiResults.majorReport.ideVersion, newProblems, newMissingProblems)
    }
  }
}
