package com.jetbrains.pluginverifier.core

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.parameters.filtering.IgnoredProblemsHolder
import com.jetbrains.pluginverifier.parameters.filtering.ProblemsFilter
import com.jetbrains.pluginverifier.reporting.verification.PluginVerificationReportage
import com.jetbrains.pluginverifier.results.deprecated.DeprecatedApiUsage
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.results.warnings.DependenciesCycleWarning

/**
 * Aggregates the plugin verification results.
 */
class VerificationResultHolder(private val pluginVerificationReportage: PluginVerificationReportage) {

  val compatibilityProblems: MutableSet<CompatibilityProblem> = hashSetOf()

  val pluginWarnings: MutableSet<PluginProblem> = hashSetOf()

  val deprecatedUsages: MutableSet<DeprecatedApiUsage> = hashSetOf()

  var dependenciesGraph: DependenciesGraph? = null

  val ignoredProblemsHolder = IgnoredProblemsHolder(pluginVerificationReportage)

  fun registerDeprecatedUsage(deprecatedApiUsage: DeprecatedApiUsage) {
    if (deprecatedApiUsage !in deprecatedUsages) {
      deprecatedUsages.add(deprecatedApiUsage)
      pluginVerificationReportage.logDeprecatedUsage(deprecatedApiUsage)
    }
  }

  fun registerProblem(problem: CompatibilityProblem) {
    if (problem !in compatibilityProblems && !ignoredProblemsHolder.isIgnored(problem)) {
      compatibilityProblems.add(problem)
      pluginVerificationReportage.logNewProblemDetected(problem)
    }
  }

  fun registerIgnoredProblem(problem: CompatibilityProblem, ignoreDecisions: List<ProblemsFilter.Result.Ignore>) {
    if (problem !in compatibilityProblems && !ignoredProblemsHolder.isIgnored(problem)) {
      ignoredProblemsHolder.registerIgnoredProblem(problem, ignoreDecisions)
    }
  }

  private fun registerWarning(warning: PluginProblem) {
    if (warning !in pluginWarnings) {
      pluginWarnings.add(warning)
      pluginVerificationReportage.logNewWarningDetected(warning)
    }
  }

  fun addCycleWarningIfExists(dependenciesGraph: DependenciesGraph) {
    val cycles = dependenciesGraph.getAllCycles()
    if (cycles.isNotEmpty()) {
      val nodes = cycles[0]
      val cyclePresentation = nodes.joinToString(separator = " -> ") + " -> " + nodes[0]
      registerWarning(DependenciesCycleWarning(cyclePresentation))
    }
  }

  fun addPluginWarnings(pluginWarnings: List<PluginProblem>) {
    pluginWarnings.forEach {
      registerWarning(it)
    }
  }


}