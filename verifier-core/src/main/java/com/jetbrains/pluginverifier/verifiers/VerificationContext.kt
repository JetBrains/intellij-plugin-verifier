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
    val verificationClassLoader: Resolver,
    val pluginResolver: Resolver,
    val dependenciesResolver: Resolver,
    val jdkClassesResolver: Resolver,
    val ideResolver: Resolver,
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
      if (shouldIndexDeprecatedClass(hostClass.className)) {
        resultHolder.registerDeprecatedUsage(deprecatedApiUsage)
      }
    }
  }

  fun getOriginOfClass(className: String): ClassFileOrigin? {
    if (pluginResolver.containsClass(className)) {
      return ClassFileOrigin.PluginInternalClass
    }
    if (jdkClassesResolver.containsClass(className)) {
      return ClassFileOrigin.JdkClass
    }
    if (ideResolver.containsClass(className)) {
      return ClassFileOrigin.IdeClass
    }
    if (dependenciesResolver.containsClass(className)) {
      return ClassFileOrigin.ClassOfPluginDependency
    }
    return null
  }

  /**
   * Determines whether we should index usage of deprecated API.
   *
   * We would like to index only usages of deprecated API of IDE and API of plugin's dependencies',
   * and exclude usages of deprecated JDK API and plugin's internal deprecated API.
   */
  private fun shouldIndexDeprecatedClass(className: String) =
      with(getOriginOfClass(className)) {
        this == ClassFileOrigin.IdeClass || this == ClassFileOrigin.ClassOfPluginDependency
      }

  private fun Location.getHostClass() = when (this) {
    is ClassLocation -> this
    is MethodLocation -> this.hostClass
    is FieldLocation -> this.hostClass
    else -> impossible()
  }

  fun isExternalClass(className: String) = externalClassesPrefixes.any { it.isNotEmpty() && className.startsWith(it) }

}