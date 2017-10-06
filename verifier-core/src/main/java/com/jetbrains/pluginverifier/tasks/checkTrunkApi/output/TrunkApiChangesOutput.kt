package com.jetbrains.pluginverifier.tasks.checkTrunkApi.output

import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import com.google.common.collect.Multimaps
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.dependencies.DependencyNode
import com.jetbrains.pluginverifier.misc.pluralize
import com.jetbrains.pluginverifier.output.teamcity.TeamCityLog
import com.jetbrains.pluginverifier.output.teamcity.TeamCityResultPrinter
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.UpdateInfo
import com.jetbrains.pluginverifier.results.Result
import com.jetbrains.pluginverifier.results.Verdict
import com.jetbrains.pluginverifier.results.problems.Problem
import com.jetbrains.pluginverifier.tasks.checkTrunkApi.result.TrunkApiChanges

/**
 * @author Sergey Patrikeev
 */
class TrunkApiChangesOutput(private val tcLog: TeamCityLog, private val repository: PluginRepository) {

  private fun TrunkApiChanges.getNewPluginProblems(): Multimap<PluginInfo, Problem> {
    val result = HashMultimap.create<PluginInfo, Problem>()
    for ((plugin, cmpResult) in comparingResults.entries) {
      val releaseProblems = cmpResult.releaseResult.verdict.getProblems()
      val trunkProblems = cmpResult.trunkResult.verdict.getProblems()
      val newProblems = trunkProblems - releaseProblems
      result.putAll(plugin, newProblems)
    }
    return result
  }

  private fun Verdict.getProblems() = when (this) {
    is Verdict.NotFound, is Verdict.Bad, is Verdict.OK, is Verdict.Warnings, is Verdict.FailedToDownload -> emptySet()
    is Verdict.MissingDependencies -> problems
    is Verdict.Problems -> problems
  }

  private fun getPluginUrl(pluginInfo: PluginInfo) = (pluginInfo as? UpdateInfo)?.let { repository.getPluginOverviewUrl(it) }

  fun printTrunkApiCompareResult(apiChanges: TrunkApiChanges) {
    val plugin2NewProblems: Multimap<PluginInfo, Problem> = apiChanges.getNewPluginProblems()
    val problem2Plugins: Multimap<Problem, PluginInfo> = Multimaps.invertFrom(plugin2NewProblems, HashMultimap.create())

    val allProblems = problem2Plugins.keySet()

    for ((problemClass, allProblemsOfClass) in allProblems.groupBy { it.javaClass }) {
      val problemTypeSuite = TeamCityResultPrinter.convertProblemClassNameToSentence(problemClass)
      tcLog.testSuiteStarted("($problemTypeSuite)").use {
        val shortDescription2Problems = allProblemsOfClass.groupBy { it.shortDescription }
        for ((shortDescription, problemsWithShortDescription) in shortDescription2Problems) {
          for (problem in problemsWithShortDescription) {
            tcLog.testSuiteStarted(shortDescription).use {
              for (plugin in problem2Plugins.get(problem)) {
                val pluginName = "($plugin)"
                tcLog.testStarted(pluginName).use {
                  val problemDetails = buildString {
                    append(problem.shortDescription)
                    append("\nThis problem takes place in ${apiChanges.trunkVersion} but not in ${apiChanges.releaseVersion}")
                    append(getMissingDependenciesDetails(apiChanges, plugin))
                  }
                  val pluginUrl = getPluginUrl(plugin)
                  val pluginUrlPart = if (pluginUrl != null) "Plugin URL: $pluginUrl\n" else ""
                  val message = pluginUrlPart + "Plugin: ${plugin.pluginId}:${plugin.version}"
                  tcLog.testFailed(pluginName, message, problemDetails)
                }
              }
            }
          }
        }
      }
    }

    val newProblemsCnt = allProblems.distinctBy { it.shortDescription }.size
    if (newProblemsCnt > 0) {
      tcLog.buildStatusFailure("$newProblemsCnt new " + "problem".pluralize(newProblemsCnt) + " detected in ${apiChanges.trunkVersion} compared to ${apiChanges.releaseVersion}")
    } else {
      tcLog.buildStatusSuccess("No new compatibility problems found in ${apiChanges.trunkVersion} compared to ${apiChanges.releaseVersion}")
    }
  }

  private fun DependenciesGraph.getResolvedDependency(dependency: PluginDependency): DependencyNode? =
      edges.find { it.dependency == dependency }?.to

  private fun Result.getResolvedDependency(dependency: PluginDependency): DependencyNode? =
      (this.verdict as? Verdict.MissingDependencies)?.dependenciesGraph?.getResolvedDependency(dependency)

  private fun getMissingDependenciesDetails(apiChanges: TrunkApiChanges, plugin: PluginInfo): String {
    val (releaseResult, trunkResult) = apiChanges.comparingResults[plugin] ?: return ""
    val releaseMissingDependencies = releaseResult.getDirectMissingDependencies()
    val trunkMissingDependencies = trunkResult.getDirectMissingDependencies()

    if (trunkMissingDependencies.isNotEmpty()) {
      return buildString {
        append("\nNote: some problems might have been caused by missing dependencies: [\n")
        for ((dependency, missingReason) in trunkMissingDependencies) {
          append("$dependency: $missingReason")

          val releaseResolvedDependency = releaseResult.getResolvedDependency(dependency)
          if (releaseResolvedDependency != null) {
            append(" (when ${apiChanges.releaseVersion} was checked, $releaseResolvedDependency was used)")
          } else {
            val releaseMissingDep = releaseMissingDependencies.find { it.dependency == dependency }
            if (releaseMissingDep != null) {
              append(" (it was also missing when we checked ${apiChanges.releaseVersion} ")
              if (missingReason == releaseMissingDep.missingReason) {
                append("by the same reason)")
              } else {
                append("by the following reason: ${releaseMissingDep.missingReason})")
              }
            }
          }
          append("\n")
        }
        append("]")
      }
    }
    return ""
  }

  private fun Result.getDirectMissingDependencies() = when (this.verdict) {
    is Verdict.MissingDependencies -> this.verdict.directMissingDependencies
    else -> emptyList()
  }

}