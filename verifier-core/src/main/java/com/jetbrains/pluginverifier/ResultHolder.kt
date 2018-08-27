package com.jetbrains.pluginverifier

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.parameters.filtering.IgnoredProblemsHolder
import com.jetbrains.pluginverifier.parameters.filtering.ProblemsFilter
import com.jetbrains.pluginverifier.results.deprecated.DeprecatedApiUsage
import com.jetbrains.pluginverifier.results.experimental.ExperimentalApiUsage
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.results.structure.PluginStructureError
import com.jetbrains.pluginverifier.results.structure.PluginStructureWarning
import com.jetbrains.pluginverifier.results.warnings.DependenciesCycleWarning

/**
 * Aggregates the plugin verification results:
 * 1) Plugin structure [errors] [pluginStructureErrors]
 * 2) Plugin structure [warnings] [pluginStructureWarnings]
 * 3) Compatibility [problems] [compatibilityProblems]
 * 4) Deprecated API [usages] [deprecatedUsages]
 * 5) Dependencies [graph] [dependenciesGraph] used during the verification
 */
class ResultHolder {

  val compatibilityProblems: MutableSet<CompatibilityProblem> = hashSetOf()

  val deprecatedUsages: MutableSet<DeprecatedApiUsage> = hashSetOf()

  val experimentalApiUsages: MutableSet<ExperimentalApiUsage> = hashSetOf()

  var dependenciesGraph: DependenciesGraph? = null

  val ignoredProblemsHolder = IgnoredProblemsHolder()

  val pluginStructureWarnings: MutableSet<PluginStructureWarning> = hashSetOf()

  val pluginStructureErrors: MutableSet<PluginStructureError> = hashSetOf()

  var failedToDownloadReason: String? = null

  var notFoundReason: String? = null

  private val pluginErrorsAndWarnings: MutableSet<PluginProblem> = hashSetOf()

  fun addDeprecatedUsage(deprecatedApiUsage: DeprecatedApiUsage) {
    deprecatedUsages.add(deprecatedApiUsage)
  }

  fun addExperimentalUsage(experimentalApiUsage: ExperimentalApiUsage) {
    experimentalApiUsages.add(experimentalApiUsage)
  }

  fun addProblem(problem: CompatibilityProblem) {
    if (problem !in compatibilityProblems && !ignoredProblemsHolder.isIgnored(problem)) {
      compatibilityProblems.add(problem)
    }
  }

  fun addIgnoredProblem(problem: CompatibilityProblem, ignoreDecisions: List<ProblemsFilter.Result.Ignore>) {
    if (problem !in compatibilityProblems && !ignoredProblemsHolder.isIgnored(problem)) {
      ignoredProblemsHolder.registerIgnoredProblem(problem, ignoreDecisions)
    }
  }

  fun addPluginErrorOrWarning(errorOrWarning: PluginProblem) {
    if (errorOrWarning !in pluginErrorsAndWarnings) {
      pluginErrorsAndWarnings.add(errorOrWarning)
      if (errorOrWarning.level == PluginProblem.Level.WARNING) {
        val pluginStructureWarning = PluginStructureWarning(errorOrWarning.message)
        pluginStructureWarnings.add(pluginStructureWarning)
      } else {
        val pluginStructureError = PluginStructureError(errorOrWarning.message)
        pluginStructureErrors.add(pluginStructureError)
      }
    }
  }

  fun addCycleWarningIfExists(dependenciesGraph: DependenciesGraph) {
    val cycles = dependenciesGraph.getAllCycles()
    if (cycles.isNotEmpty()) {
      val nodes = cycles[0]
      val cyclePresentation = nodes.joinToString(separator = " -> ") + " -> " + nodes[0]
      addPluginErrorOrWarning(DependenciesCycleWarning(cyclePresentation))
    }
  }


}