package com.jetbrains.pluginverifier.verifiers

import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.core.VerificationResultHolder
import com.jetbrains.pluginverifier.parameters.filtering.ProblemsFilter
import com.jetbrains.pluginverifier.results.deprecated.DeprecatedApiUsage
import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.location.FieldLocation
import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileOrigin
import com.jetbrains.pluginverifier.verifiers.resolution.ClsResolver

data class VerificationContext(
    val plugin: IdePlugin,
    val ideVersion: IdeVersion,
    val resultHolder: VerificationResultHolder,
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
      val deprecatedElement = deprecatedApiUsage.deprecatedElement
      val hostClass = deprecatedElement.getHostClass()
      if (shouldIndexDeprecatedClass(hostClass.className)) {
        resultHolder.registerDeprecatedUsage(deprecatedApiUsage)
      }
    }
  }

  /**
   * Determines whether we should index usage of deprecated API.
   *
   * We would like to index only usages of deprecated API of IDE and API of plugin's dependencies',
   * and exclude usages of deprecated JDK API and plugin's internal deprecated API.
   */
  private fun shouldIndexDeprecatedClass(className: String) =
      with(clsResolver.getOriginOfClass(className)) {
        this == ClassFileOrigin.IdeClass || this == ClassFileOrigin.ClassOfPluginDependency
      }

  private fun Location.getHostClass() = when (this) {
    is ClassLocation -> this
    is MethodLocation -> this.hostClass
    is FieldLocation -> this.hostClass
  }

}