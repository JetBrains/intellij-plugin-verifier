package com.jetbrains.pluginverifier.verifiers

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.core.VerificationResultHolder
import com.jetbrains.pluginverifier.misc.impossible
import com.jetbrains.pluginverifier.parameters.filtering.ProblemsFilter
import com.jetbrains.pluginverifier.results.deprecated.DeprecatedApiUsage
import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.location.FieldLocation
import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem

data class VerificationContext(
    val plugin: IdePlugin,
    val ideVersion: IdeVersion,
    val classLoader: Resolver,
    val ideClassLoader: Resolver,
    val resultHolder: VerificationResultHolder,
    val externalClassesPrefixes: List<String>,
    val findDeprecatedApiUsages: Boolean,
    val problemFilters: List<ProblemsFilter>
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
      if (isIdeClass(hostClass.className)) {
        resultHolder.registerDeprecatedUsage(deprecatedApiUsage)
      }
    }
  }

  private fun Location.getHostClass(): ClassLocation = when (this) {
    is ClassLocation -> this
    is MethodLocation -> this.hostClass
    is FieldLocation -> this.hostClass
    else -> impossible()
  }

  fun isIdeClass(className: String): Boolean = ideClassLoader.containsClass(className)

  fun isExternalClass(className: String): Boolean = externalClassesPrefixes.any { it.isNotEmpty() && className.startsWith(it) }

}