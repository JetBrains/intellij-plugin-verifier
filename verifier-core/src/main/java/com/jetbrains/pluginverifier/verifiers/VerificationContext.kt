package com.jetbrains.pluginverifier.verifiers

import com.jetbrains.pluginverifier.ResultHolder
import com.jetbrains.pluginverifier.VerificationTarget
import com.jetbrains.pluginverifier.parameters.filtering.ProblemsFilter
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.results.deprecated.DeprecatedApiUsage
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
    val findDeprecatedApiUsages: Boolean,
    val problemFilters: List<ProblemsFilter>,
    val clsResolver: ClsResolver
) {

  fun registerProblem(problem: CompatibilityProblem) {
    val shouldReportDecisions = problemFilters.map { it.shouldReportProblem(problem, this) }
    val ignoreDecisions = shouldReportDecisions.filterIsInstance<ProblemsFilter.Result.Ignore>()
    if (ignoreDecisions.isNotEmpty()) {
      resultHolder.registerIgnoredProblem(problem, ignoreDecisions)
    } else {
      resultHolder.registerProblem(problem)
    }
  }

  fun registerDeprecatedUsage(deprecatedApiUsage: DeprecatedApiUsage) {
    if (findDeprecatedApiUsages) {
      val deprecatedElementHost = deprecatedApiUsage.deprecatedElement.getHostClass()
      val usageHostClass = deprecatedApiUsage.usageLocation.getHostClass()
      if (shouldIndexDeprecatedClass(deprecatedElementHost, usageHostClass)) {
        resultHolder.registerDeprecatedUsage(deprecatedApiUsage)
      }
    }
  }

  /**
   * Determines whether we should index usage of deprecated API:
   * 1) Usage resides in plugin
   * 2) API is either IDE API or plugin's dependency API,
   * and it is not deprecated JDK API nor plugin's internal
   * deprecated API.
   */
  private fun shouldIndexDeprecatedClass(
      deprecatedElementHost: ClassLocation,
      usageHostClass: ClassLocation
  ): Boolean {
    val usageHostOrigin = clsResolver.getOriginOfClass(usageHostClass.className)
    if (usageHostOrigin == ClassFileOrigin.PLUGIN_INTERNAL_CLASS) {
      val deprecatedHostOrigin = clsResolver.getOriginOfClass(deprecatedElementHost.className)
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