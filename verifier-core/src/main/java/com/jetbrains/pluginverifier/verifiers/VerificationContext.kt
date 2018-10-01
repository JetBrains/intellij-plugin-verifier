package com.jetbrains.pluginverifier.verifiers

import com.jetbrains.pluginverifier.ResultHolder
import com.jetbrains.pluginverifier.VerificationTarget
import com.jetbrains.pluginverifier.parameters.filtering.ProblemsFilter
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.results.deprecated.DeprecatedApiUsage
import com.jetbrains.pluginverifier.results.experimental.ExperimentalApiUsage
import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.location.FieldLocation
import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileOrigin
import com.jetbrains.pluginverifier.verifiers.resolution.ClsResolver

data class VerificationContext(
    val plugin: PluginInfo,
    val verificationTarget: VerificationTarget,
    val resultHolder: ResultHolder,
    val findUnstableApiUsages: Boolean,
    val problemFilters: List<ProblemsFilter>,
    val clsResolver: ClsResolver
) : ProblemRegistrar {

  override fun registerProblem(problem: CompatibilityProblem) {
    val shouldReportDecisions = problemFilters.map { it.shouldReportProblem(problem, this) }
    val ignoreDecisions = shouldReportDecisions.filterIsInstance<ProblemsFilter.Result.Ignore>()
    if (ignoreDecisions.isNotEmpty()) {
      resultHolder.addIgnoredProblem(problem, ignoreDecisions)
    } else {
      resultHolder.addProblem(problem)
    }
  }

  override fun registerDeprecatedUsage(deprecatedApiUsage: DeprecatedApiUsage) {
    if (findUnstableApiUsages) {
      val deprecatedElementHost = deprecatedApiUsage.apiElement.getHostClass()
      val usageHostClass = deprecatedApiUsage.usageLocation.getHostClass()
      if (shouldIndexClass(deprecatedElementHost, usageHostClass)) {
        resultHolder.addDeprecatedUsage(deprecatedApiUsage)
      }
    }
  }

  override fun registerExperimentalApiUsage(experimentalApiUsage: ExperimentalApiUsage) {
    if (findUnstableApiUsages) {
      val elementHostClass = experimentalApiUsage.apiElement.getHostClass()
      val usageHostClass = experimentalApiUsage.usageLocation.getHostClass()
      if (shouldIndexClass(elementHostClass, usageHostClass)) {
        resultHolder.addExperimentalUsage(experimentalApiUsage)
      }
    }
  }

  /**
   * Determines whether we should index usage of API.
   *
   * The following two conditions must be met:
   * 1) The usage resides in plugin
   * 2) API is either IDE API or plugin's dependency API,
   * and it is not deprecated JDK API nor plugin's internal
   * deprecated API.
   */
  private fun shouldIndexClass(
      elementHost: ClassLocation,
      usageHostClass: ClassLocation
  ): Boolean {
    val usageHostOrigin = clsResolver.getOriginOfClass(usageHostClass.className)
    if (usageHostOrigin == ClassFileOrigin.PLUGIN_INTERNAL_CLASS) {
      val deprecatedHostOrigin = clsResolver.getOriginOfClass(elementHost.className)
      return deprecatedHostOrigin == ClassFileOrigin.IDE_CLASS || deprecatedHostOrigin == ClassFileOrigin.CLASS_OF_PLUGIN_DEPENDENCY
    }
    return false
  }

  private fun Location.getHostClass() = when (this) {
    is ClassLocation -> this
    is MethodLocation -> this.hostClass
    is FieldLocation -> this.hostClass
  }

  override fun toString() = "Verification context for $plugin against $verificationTarget"

}