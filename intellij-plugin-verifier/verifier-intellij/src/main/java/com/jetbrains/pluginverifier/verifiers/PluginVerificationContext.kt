package com.jetbrains.pluginverifier.verifiers

import com.jetbrains.pluginverifier.ResultHolder
import com.jetbrains.pluginverifier.VerificationTarget
import com.jetbrains.pluginverifier.parameters.filtering.ProblemsFilter
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.location.FieldLocation
import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.usages.ApiUsageProcessor
import com.jetbrains.pluginverifier.usages.deprecated.DeprecatedApiRegistrar
import com.jetbrains.pluginverifier.usages.deprecated.DeprecatedApiUsage
import com.jetbrains.pluginverifier.usages.discouraging.DiscouragingJdkClassUsage
import com.jetbrains.pluginverifier.usages.experimental.ExperimentalApiRegistrar
import com.jetbrains.pluginverifier.usages.experimental.ExperimentalApiUsage
import com.jetbrains.pluginverifier.verifiers.resolution.ClassResolver
import com.jetbrains.pluginverifier.verifiers.resolution.IntelliJClassFileOrigin

data class PluginVerificationContext(
    val plugin: PluginInfo,
    val verificationTarget: VerificationTarget,
    val resultHolder: ResultHolder,
    val checkApiUsages: Boolean,
    val problemFilters: List<ProblemsFilter>,
    override val classResolver: ClassResolver,
    override val apiUsageProcessors: List<ApiUsageProcessor>
) : VerificationContext, ProblemRegistrar, DeprecatedApiRegistrar, ExperimentalApiRegistrar {

  override val problemRegistrar
    get() = this

  override val allProblems
    get() = resultHolder.compatibilityProblems

  override fun registerProblem(problem: CompatibilityProblem) {
    val shouldReportDecisions = problemFilters.map { it.shouldReportProblem(problem, this) }
    val ignoreDecisions = shouldReportDecisions.filterIsInstance<ProblemsFilter.Result.Ignore>()
    if (ignoreDecisions.isNotEmpty()) {
      resultHolder.addIgnoredProblem(problem, ignoreDecisions)
    } else {
      resultHolder.addProblem(problem)
    }
  }

  override fun unregisterProblem(problem: CompatibilityProblem) {
    resultHolder.compatibilityProblems -= problem
  }

  override fun registerDeprecatedUsage(deprecatedApiUsage: DeprecatedApiUsage) {
    if (checkApiUsages) {
      val deprecatedElementHost = deprecatedApiUsage.apiElement.getHostClass()
      val usageHostClass = deprecatedApiUsage.usageLocation.getHostClass()
      if (deprecatedApiUsage is DiscouragingJdkClassUsage || shouldIndexDeprecatedClass(usageHostClass, deprecatedElementHost)) {
        resultHolder.addDeprecatedUsage(deprecatedApiUsage)
      }
    }
  }

  override fun registerExperimentalApiUsage(experimentalApiUsage: ExperimentalApiUsage) {
    if (checkApiUsages) {
      val elementHostClass = experimentalApiUsage.apiElement.getHostClass()
      val usageHostClass = experimentalApiUsage.usageLocation.getHostClass()
      if (shouldIndexDeprecatedClass(usageHostClass, elementHostClass)) {
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
  private fun shouldIndexDeprecatedClass(usageHostClass: ClassLocation, apiHostClass: ClassLocation): Boolean {
    val usageHostOrigin = usageHostClass.classFileOrigin
    if (usageHostOrigin is IntelliJClassFileOrigin.PluginClass) {
      val apiHostOrigin = apiHostClass.classFileOrigin
      return apiHostOrigin is IntelliJClassFileOrigin.IdeClass || apiHostOrigin is IntelliJClassFileOrigin.ClassOfPluginDependency
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