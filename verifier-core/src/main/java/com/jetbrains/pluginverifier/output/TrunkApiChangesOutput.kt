package com.jetbrains.pluginverifier.output

import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.pluginverifier.api.PluginInfo
import com.jetbrains.pluginverifier.api.Result
import com.jetbrains.pluginverifier.api.Verdict
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.dependencies.DependencyNode
import com.jetbrains.pluginverifier.misc.pluralize
import com.jetbrains.pluginverifier.problems.Problem
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.RepositoryManager
import com.jetbrains.pluginverifier.tasks.TrunkApiChanges

/**
 * @author Sergey Patrikeev
 */
class TrunkApiChangesOutput(private val tcLog: TeamCityLog, private val repository: PluginRepository = RepositoryManager) {

  private fun TrunkApiChanges.getNewProblems(): Multimap<Problem, PluginInfo> {
    val result = HashMultimap.create<Problem, PluginInfo>()
    for ((plugin, cmpResult) in comparingResults.entries) {
      val releaseProblems = cmpResult.releaseResult.verdict.getProblems()
      val trunkProblems = cmpResult.trunkResult.verdict.getProblems()
      val newProblems = trunkProblems - releaseProblems
      newProblems.forEach { result.put(it, plugin) }
    }
    return result
  }

  private fun Verdict.getProblems() = when (this) {
    is Verdict.NotFound, is Verdict.Bad, is Verdict.OK, is Verdict.Warnings -> emptySet()
    is Verdict.MissingDependencies -> problems
    is Verdict.Problems -> problems
  }

  private fun getPluginUrl(pluginInfo: PluginInfo) = pluginInfo.updateInfo?.let { repository.getPluginOverviewUrl(it) }

  fun printTrunkApiCompareResult(apiChanges: TrunkApiChanges) {
    val problemToPlugins: Multimap<Problem, PluginInfo> = apiChanges.getNewProblems()

    val allProblems = problemToPlugins.keySet()

    val releaseVersion = apiChanges.releaseVersion
    val trunkVersion = apiChanges.trunkVersion

    for ((problemClass, allProblemsOfClass) in allProblems.groupBy { it.javaClass }) {
      val problemTypeSuite = TeamCityPrinter.convertProblemClassNameToSentence(problemClass)
      tcLog.testSuiteStarted("($problemTypeSuite)").use {
        for ((shortDescription, problemsWithShortDescription) in allProblemsOfClass.groupBy { it.getShortDescription() }) {
          for (problem in problemsWithShortDescription) {
            tcLog.testSuiteStarted(shortDescription.toString()).use {
              for (plugin in problemToPlugins.get(problem)) {
                val pluginName = "($plugin)"
                tcLog.testStarted(pluginName).use {
                  val problemDetails = buildString {
                    append(problem.getFullDescription().toString())
                    append("\nThis problem takes place in $trunkVersion but not in $releaseVersion")
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

    val newProblemsCnt = allProblems.distinctBy { it.getShortDescription() }.size
    if (newProblemsCnt > 0) {
      tcLog.buildStatusFailure("$newProblemsCnt new " + "problem".pluralize(newProblemsCnt) + " detected in $trunkVersion compared to $releaseVersion")
    } else {
      tcLog.buildStatusSuccess("No new compatibility problems found in $trunkVersion compared to $releaseVersion")
    }
  }

  private fun DependenciesGraph.getResolvedDependency(dependency: PluginDependency): DependencyNode? =
      edges.find { it.dependency == dependency }?.to

  private fun Result.getResolvedDependency(dependency: PluginDependency): DependencyNode? =
      (this.verdict as? Verdict.MissingDependencies)?.dependenciesGraph?.getResolvedDependency(dependency)

  private fun getMissingDependenciesDetails(apiChanges: TrunkApiChanges, plugin: PluginInfo): String {
    val (releaseResult, trunkResult) = apiChanges.comparingResults[plugin] ?: return ""
    val releaseMissingDependencies = releaseResult.getMissingDependencies()
    val trunkMissingDependencies = trunkResult.getMissingDependencies()

    if (trunkMissingDependencies.isNotEmpty()) {
      return buildString {
        append("\nNote: some problems might have been caused by missing dependencies: [\n")
        for ((dependency, missingReason) in trunkMissingDependencies) {
          append("$dependency: $missingReason")

          val releaseResolvedDependency = releaseResult.getResolvedDependency(dependency)
          if (releaseResolvedDependency != null) {
            append(" (when checked ${apiChanges.releaseVersion} plugin $releaseResolvedDependency was used)")
          } else {
            val releaseMissingDep = releaseMissingDependencies.find { it.dependency == dependency } ?: continue
            append(" (it was also missing when we checked ${apiChanges.releaseVersion} ")
            if (missingReason == releaseMissingDep.missingReason) {
              append("by the same reason)")
            } else {
              append("by the following reason: ${releaseMissingDep.missingReason})")
            }
          }
          append("\n")
        }
        append("]")
      }
    }
    return ""
  }

  private fun Result.getMissingDependencies() = when (this.verdict) {
    is Verdict.MissingDependencies -> this.verdict.missingDependencies
    else -> emptyList()
  }

}