package com.jetbrains.pluginverifier

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.parameters.filtering.IgnoredProblemsHolder
import com.jetbrains.pluginverifier.parameters.filtering.ProblemsFilter
import com.jetbrains.pluginverifier.reporting.verification.PluginVerificationReportage
import com.jetbrains.pluginverifier.results.deprecated.DeprecatedApiUsage
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.results.structure.PluginStructureError
import com.jetbrains.pluginverifier.results.structure.PluginStructureWarning
import com.jetbrains.pluginverifier.results.warnings.DependenciesCycleWarning

/**
 * Aggregates the plugin verification results:
 * 1) Plugin structure [errors] [pluginStructureErrors]
 * 2) Plugin structure [warnings] [pluginStructureWarnings]
 * 3) Binary compatibility [problems] [compatibilityProblems]
 * 4) Deprecated API [usages] [deprecatedUsages]
 * 5) Dependencies [graph] [dependenciesGraph] used during the verification
 */
class ResultHolder(private val pluginVerificationReportage: PluginVerificationReportage) {

  val compatibilityProblems: MutableSet<CompatibilityProblem> = hashSetOf()

  val deprecatedUsages: MutableSet<DeprecatedApiUsage> = hashSetOf()

  var dependenciesGraph: DependenciesGraph? = null

  val ignoredProblemsHolder = IgnoredProblemsHolder(pluginVerificationReportage)

  val pluginStructureWarnings: MutableSet<PluginStructureWarning> = hashSetOf()

  val pluginStructureErrors: MutableSet<PluginStructureError> = hashSetOf()

  var failedToDownloadReason: String? = null

  var notFoundReason: String? = null

  private val pluginErrorsAndWarnings: MutableSet<PluginProblem> = hashSetOf()

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

  fun registerPluginErrorOrWarning(errorOrWarning: PluginProblem) {
    if (errorOrWarning !in pluginErrorsAndWarnings) {
      pluginErrorsAndWarnings.add(errorOrWarning)
      if (errorOrWarning.level == PluginProblem.Level.WARNING) {
        val pluginStructureWarning = PluginStructureWarning(errorOrWarning.message)
        pluginStructureWarnings.add(pluginStructureWarning)
        pluginVerificationReportage.logNewPluginStructureWarning(pluginStructureWarning)
      } else {
        val pluginStructureError = PluginStructureError(errorOrWarning.message)
        pluginStructureErrors.add(pluginStructureError)
        pluginVerificationReportage.logNewPluginStructureError(pluginStructureError)
      }
    }
  }

  fun addCycleWarningIfExists(dependenciesGraph: DependenciesGraph) {
    val cycles = dependenciesGraph.getAllCycles()
    if (cycles.isNotEmpty()) {
      val nodes = cycles[0]
      val cyclePresentation = nodes.joinToString(separator = " -> ") + " -> " + nodes[0]
      registerPluginErrorOrWarning(DependenciesCycleWarning(cyclePresentation))
    }
  }


}