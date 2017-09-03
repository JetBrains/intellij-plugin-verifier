package com.jetbrains.pluginverifier.tasks

import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import com.intellij.structure.ide.IdeVersion
import com.jetbrains.pluginverifier.api.PluginInfo
import com.jetbrains.pluginverifier.api.Verdict
import com.jetbrains.pluginverifier.dependencies.MissingDependency
import com.jetbrains.pluginverifier.problems.Problem

data class CheckTrunkApiCompareResult(val trunkVersion: IdeVersion,
                                      val releaseVersion: IdeVersion,
                                      val newProblemToPlugin: Multimap<Problem, PluginInfo>,
                                      val newMissingProblems: Multimap<MissingDependency, PluginInfo>) {
  companion object {

    fun create(releaseResult: CheckIdeResult, trunkResult: CheckIdeResult): CheckTrunkApiCompareResult {
      val missingDep2Plugin = HashMultimap.create<MissingDependency, PluginInfo>()
      val problem2Plugin = HashMultimap.create<Problem, PluginInfo>()

      val trunkPlugin2Result = trunkResult.results.associateBy { it.plugin }
      val releasePlugin2Result = releaseResult.results.associateBy { it.plugin }

      for ((plugin, newResult) in trunkPlugin2Result) {
        val oldResult = releasePlugin2Result[plugin] ?: continue
        val oldVerdict = oldResult.verdict
        val newVerdict = newResult.verdict
        if (oldVerdict is Verdict.NotFound || newVerdict is Verdict.NotFound) {
          continue
        }

        val oldMissingDeps = getMissingDependenciesOfVerdict(oldVerdict)
        val newMissingDeps = getMissingDependenciesOfVerdict(newVerdict)
        (newMissingDeps - oldMissingDeps).forEach {
          missingDep2Plugin.put(it, plugin)
        }

        val oldProblems = getProblemsOfVerdict(oldVerdict)
        val newProblems = getProblemsOfVerdict(newVerdict)
        (newProblems - oldProblems).forEach {
          problem2Plugin.put(it, plugin)
        }
      }

      return CheckTrunkApiCompareResult(trunkResult.ideVersion, releaseResult.ideVersion, problem2Plugin, missingDep2Plugin)
    }

    private fun getMissingDependenciesOfVerdict(verdict: Verdict) = when (verdict) {
      is Verdict.MissingDependencies -> verdict.missingDependencies
      else -> emptyList()
    }

    private fun getProblemsOfVerdict(verdict: Verdict) = when (verdict) {
      is Verdict.OK, is Verdict.Warnings, is Verdict.Bad, is Verdict.NotFound -> emptySet()
      is Verdict.Problems -> verdict.problems
      is Verdict.MissingDependencies -> verdict.problems
    }
  }
}