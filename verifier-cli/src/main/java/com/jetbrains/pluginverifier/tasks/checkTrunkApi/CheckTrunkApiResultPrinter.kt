package com.jetbrains.pluginverifier.tasks.checkTrunkApi

import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import com.google.common.collect.Multimaps
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.dependencies.DependencyNode
import com.jetbrains.pluginverifier.dependencies.MissingDependency
import com.jetbrains.pluginverifier.misc.pluralize
import com.jetbrains.pluginverifier.misc.replaceInvalidFileNameCharacters
import com.jetbrains.pluginverifier.output.OutputOptions
import com.jetbrains.pluginverifier.output.html.HtmlResultPrinter
import com.jetbrains.pluginverifier.output.teamcity.TeamCityLog
import com.jetbrains.pluginverifier.output.teamcity.TeamCityResultPrinter
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.UpdateInfo
import com.jetbrains.pluginverifier.results.Result
import com.jetbrains.pluginverifier.results.Verdict
import com.jetbrains.pluginverifier.results.problems.Problem
import com.jetbrains.pluginverifier.tasks.TaskResult
import com.jetbrains.pluginverifier.tasks.TaskResultPrinter

/**
 * @author Sergey Patrikeev
 */
class CheckTrunkApiResultPrinter(private val outputOptions: OutputOptions,
                                 private val pluginRepository: PluginRepository) : TaskResultPrinter {

  override fun printResults(taskResult: TaskResult) {
    with(taskResult as CheckTrunkApiResult) {
      if (outputOptions.needTeamCityLog) {
        printTrunkApiCompareResult(this)
      }
      saveToHtmlFile(releaseIdeVersion, releaseResults)
      saveToHtmlFile(trunkIdeVersion, trunkResults)
    }
  }

  private fun saveToHtmlFile(ideVersion: IdeVersion, results: List<Result>) {
    val htmlReportFile = outputOptions.verificationReportsDirectory
        .resolve(ideVersion.toString().replaceInvalidFileNameCharacters())
        .resolve("report.html")

    val htmlResultPrinter = HtmlResultPrinter(listOf(ideVersion), { false }, htmlReportFile, outputOptions.missingDependencyIgnoring)
    htmlResultPrinter.printResults(results)
  }

  private fun CheckTrunkApiResult.getNewPluginProblems(): Multimap<PluginInfo, Problem> {
    val result = HashMultimap.create<PluginInfo, Problem>()
    for ((plugin, cmp) in comparingResults) {
      result.putAll(plugin, cmp.getNewApiProblems())
    }
    return result
  }

  private fun printTrunkApiCompareResult(apiChanges: CheckTrunkApiResult) {
    val tcLog = TeamCityLog(System.out)

    val plugin2NewProblems = apiChanges.getNewPluginProblems()
    val problem2Plugins = Multimaps.invertFrom(plugin2NewProblems, HashMultimap.create<Problem, PluginInfo>())

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
                    append(problem.fullDescription)
                    append("\nThis problem takes place in ${apiChanges.trunkIdeVersion} but not in ${apiChanges.releaseIdeVersion}")
                    append(getMissingDependenciesDetails(apiChanges, plugin))
                  }
                  val pluginUrl = (plugin as? UpdateInfo)?.browserURL
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
      tcLog.buildStatusFailure("$newProblemsCnt new " + "problem".pluralize(newProblemsCnt) + " detected in ${apiChanges.trunkIdeVersion} compared to ${apiChanges.releaseIdeVersion}")
    } else {
      tcLog.buildStatusSuccess("No new compatibility problems found in ${apiChanges.trunkIdeVersion} compared to ${apiChanges.releaseIdeVersion}")
    }
  }

  private fun DependenciesGraph.getResolvedDependency(dependency: PluginDependency): DependencyNode? =
      edges.find { it.dependency == dependency }?.to

  private fun Result.getResolvedDependency(dependency: PluginDependency): DependencyNode? =
      (this.verdict as? Verdict.MissingDependencies)?.dependenciesGraph?.getResolvedDependency(dependency)

  private fun getMissingDependenciesDetails(apiChanges: CheckTrunkApiResult, plugin: PluginInfo): String {
    val (_, releaseResult, trunkResult) = apiChanges.comparingResults[plugin] ?: return ""
    val releaseMissingDependencies = releaseResult.getDirectMissingDependencies()
    val trunkMissingDependencies = trunkResult.getDirectMissingDependencies()

    if (trunkMissingDependencies.isNotEmpty()) {
      return buildString {
        append("\nNote: some problems might have been caused by missing dependencies: [\n")
        for ((dependency, missingReason) in trunkMissingDependencies) {
          append("$dependency: $missingReason")

          val releaseResolvedDependency = releaseResult.getResolvedDependency(dependency)
          if (releaseResolvedDependency != null) {
            append(" (when ${apiChanges.releaseIdeVersion} was checked, $releaseResolvedDependency was used)")
          } else {
            val releaseMissingDep = releaseMissingDependencies.find { it.dependency == dependency }
            if (releaseMissingDep != null) {
              append(" (it was also missing when we checked ${apiChanges.releaseIdeVersion} ")
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

  private fun Result.getDirectMissingDependencies(): List<MissingDependency> {
    val verdict = this.verdict
    return when (verdict) {
      is Verdict.MissingDependencies -> verdict.directMissingDependencies
      else -> emptyList()
    }
  }


}