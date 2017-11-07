package com.jetbrains.pluginverifier.core

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.parameters.filtering.ProblemsFilter
import com.jetbrains.pluginverifier.reporting.verification.PluginVerificationReportage
import com.jetbrains.pluginverifier.results.deprecated.DeprecatedApiUsage
import com.jetbrains.pluginverifier.results.problems.Problem
import com.jetbrains.pluginverifier.results.warnings.Warning

/**
 * @author Sergey Patrikeev
 */
class VerificationResultHolder(private val pluginVerificationReportage: PluginVerificationReportage) {

  val problems: MutableSet<Problem> = hashSetOf()

  val warnings: MutableSet<Warning> = hashSetOf()

  val deprecatedUsages: MutableSet<DeprecatedApiUsage> = hashSetOf()

  private var dependenciesGraph: DependenciesGraph? = null

  val ignoredProblems = hashSetOf<Problem>()

  fun setDependenciesGraph(graph: DependenciesGraph) {
    dependenciesGraph = graph
    pluginVerificationReportage.logDependencyGraph(graph)
    addCycleWarningIfExists(graph)
  }

  fun getDependenciesGraph(): DependenciesGraph = dependenciesGraph!!

  fun registerDeprecatedUsage(deprecatedApiUsage: DeprecatedApiUsage) {
    if (deprecatedApiUsage !in deprecatedUsages) {
      deprecatedUsages.add(deprecatedApiUsage)
      pluginVerificationReportage.logDeprecatedUsage(deprecatedApiUsage)
    }
  }

  fun registerProblem(problem: Problem) {
    if (problem !in problems && problem !in ignoredProblems) {
      problems.add(problem)
      pluginVerificationReportage.logNewProblemDetected(problem)
    }
  }

  fun registerIgnoredProblem(problem: Problem, ignoreDecisions: List<ProblemsFilter.Result.Ignore>) {
    if (problem !in problems && problem !in ignoredProblems) {
      ignoredProblems.add(problem)
      ignoreDecisions.forEach { pluginVerificationReportage.logProblemIgnored(problem, it.reason) }
    }
  }

  private fun registerWarning(warning: Warning) {
    if (warning !in warnings) {
      warnings.add(warning)
      pluginVerificationReportage.logNewWarningDetected(warning)
    }
  }

  private fun addCycleWarningIfExists(dependenciesGraph: DependenciesGraph) {
    val cycles = dependenciesGraph.getCycles()
    if (cycles.isNotEmpty()) {
      val nodes = cycles[0]
      val cycle = nodes.joinToString(separator = " -> ") + " -> " + nodes[0]
      registerWarning(Warning("The plugin ${dependenciesGraph.start} is on the dependencies cycle: $cycle"))
    }
  }

  fun addPluginWarnings(pluginWarnings: List<PluginProblem>) {
    pluginWarnings.forEach {
      registerWarning(Warning(it.message))
    }
  }


}