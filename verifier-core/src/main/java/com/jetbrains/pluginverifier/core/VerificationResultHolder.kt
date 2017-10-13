package com.jetbrains.pluginverifier.core

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.parameters.filtering.ProblemsFilter
import com.jetbrains.pluginverifier.reporting.verification.PluginVerificationReportage
import com.jetbrains.pluginverifier.results.deprecated.DeprecatedApiUsage
import com.jetbrains.pluginverifier.results.problems.Problem
import com.jetbrains.pluginverifier.results.warnings.Warning

/**
 * @author Sergey Patrikeev
 */
class VerificationResultHolder(private val idePlugin: IdePlugin,
                               private val ideVersion: IdeVersion,
                               private val problemFilters: List<ProblemsFilter>,
                               private val pluginVerificationReportage: PluginVerificationReportage) {

  val problems: MutableSet<Problem> = hashSetOf()

  val warnings: MutableSet<Warning> = hashSetOf()

  val deprecatedUsages: MutableSet<DeprecatedApiUsage> = hashSetOf()

  private var dependenciesGraph: DependenciesGraph? = null

  private val ignoredProblems = hashSetOf<Problem>()

  fun setDependenciesGraph(graph: DependenciesGraph) {
    dependenciesGraph = graph
    pluginVerificationReportage.logDependencyGraph(graph)
    addCycleWarningIfExists(graph)
  }

  fun getDependenciesGraph(): DependenciesGraph = dependenciesGraph!!

  fun registerDeprecatedUsage(deprecatedApiUsage: DeprecatedApiUsage) {
    deprecatedUsages.add(deprecatedApiUsage)
    pluginVerificationReportage.logDeprecatedUsage(deprecatedApiUsage)
  }

  fun registerProblem(problem: Problem) {
    if (problem !in problems && problem !in ignoredProblems) {
      val shouldReportDecisions = problemFilters.map { it.shouldReportProblem(idePlugin, ideVersion, problem) }
      val ignoreDecisions = shouldReportDecisions.filterIsInstance<ProblemsFilter.Result.Ignore>()

      if (ignoreDecisions.isNotEmpty()) {
        ignoredProblems.add(problem)
        ignoreDecisions.forEach { pluginVerificationReportage.logProblemIgnored(problem, it.reason) }
      } else {
        problems.add(problem)
        pluginVerificationReportage.logNewProblemDetected(problem)
      }
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
      registerWarning(Warning("The plugin $idePlugin is on the dependencies cycle: $cycle"))
    }
  }

  fun addPluginWarnings(pluginWarnings: List<PluginProblem>) {
    pluginWarnings.forEach {
      registerWarning(Warning(it.message))
    }
  }


}