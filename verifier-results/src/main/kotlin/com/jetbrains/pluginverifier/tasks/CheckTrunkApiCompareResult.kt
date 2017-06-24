package com.jetbrains.pluginverifier.tasks

import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import com.google.common.collect.Multimaps
import com.google.gson.annotations.SerializedName
import com.intellij.structure.ide.IdeVersion
import com.jetbrains.pluginverifier.api.PluginInfo
import com.jetbrains.pluginverifier.api.Result
import com.jetbrains.pluginverifier.api.Verdict
import com.jetbrains.pluginverifier.dependencies.MissingDependency
import com.jetbrains.pluginverifier.problems.Problem

data class CheckTrunkApiCompareResult(@SerializedName("trunkVersion") val trunkVersion: IdeVersion,
                                      @SerializedName("releaseVersion") val releaseVersion: IdeVersion,
                                      @SerializedName("newProblemToPlugin") val newProblemToPlugin: Multimap<Problem, PluginInfo>,
                                      @SerializedName("newMissingProblems") val newMissingProblems: Multimap<MissingDependency, PluginInfo>) {
  companion object {

    private fun getProblemsOfAllResults(results: List<Result>): Multimap<Problem, PluginInfo> {
      val problemToPlugin = HashMultimap.create<Problem, PluginInfo>()
      results.forEach { (plugin, _, verdict) ->
        when (verdict) {
          is Verdict.MissingDependencies -> {
            verdict.problems.forEach { problemToPlugin.put(it, plugin) }
          }
          is Verdict.Problems -> {
            verdict.problems.forEach { problemToPlugin.put(it, plugin) }
          }
        }
      }
      return problemToPlugin
    }

    private fun getMissingDependenciesOfAllResults(results: List<Result>): Multimap<MissingDependency, PluginInfo> {
      val missingDependencyToDependentPlugins = HashMultimap.create<MissingDependency, PluginInfo>()
      results.forEach { (plugin, _, verdict) ->
        if (verdict is Verdict.MissingDependencies) {
          verdict.missingDependencies.forEach { missingDependencyToDependentPlugins.put(it, plugin) }
        }
      }
      return missingDependencyToDependentPlugins
    }

    fun create(trunkApiResults: CheckTrunkApiResult): CheckTrunkApiCompareResult {
      val trunkProblemToPlugin: Multimap<Problem, PluginInfo> = getProblemsOfAllResults(trunkApiResults.trunkResults.results)
      val releaseProblemToPlugin: Multimap<Problem, PluginInfo> = getProblemsOfAllResults(trunkApiResults.releaseResults.results)

      val trunkMissingDepToPlugin: Multimap<MissingDependency, PluginInfo> = getMissingDependenciesOfAllResults(trunkApiResults.trunkResults.results)
      val releaseMissingDepToPlugin: Multimap<MissingDependency, PluginInfo> = getMissingDependenciesOfAllResults(trunkApiResults.releaseResults.results)

      val newProblemToPlugin = Multimaps.filterKeys(trunkProblemToPlugin, { problem -> problem !in releaseProblemToPlugin.keySet() })
      val newMissingDepToPlugin = Multimaps.filterKeys(trunkMissingDepToPlugin, { dependency -> dependency !in releaseMissingDepToPlugin.keySet() })

      return CheckTrunkApiCompareResult(trunkApiResults.trunkResults.ideVersion, trunkApiResults.releaseResults.ideVersion, newProblemToPlugin, newMissingDepToPlugin)
    }
  }
}