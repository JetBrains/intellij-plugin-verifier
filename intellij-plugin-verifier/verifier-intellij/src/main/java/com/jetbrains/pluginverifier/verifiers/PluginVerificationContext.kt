package com.jetbrains.pluginverifier.verifiers

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.classes.resolvers.findOriginOfType
import com.jetbrains.plugin.structure.classes.resolvers.isOriginOfType
import com.jetbrains.plugin.structure.ide.classes.IdeClassFileOrigin
import com.jetbrains.plugin.structure.intellij.classes.locator.PluginClassFileOrigin
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.pluginverifier.VerificationTarget
import com.jetbrains.pluginverifier.parameters.filtering.IgnoredProblemsHolder
import com.jetbrains.pluginverifier.parameters.filtering.ProblemsFilter
import com.jetbrains.pluginverifier.results.NoExplicitDependencyOnJavaPluginWarning
import com.jetbrains.pluginverifier.results.VerificationResult
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
import com.jetbrains.pluginverifier.usages.internal.InternalApiRegistrar
import com.jetbrains.pluginverifier.usages.internal.InternalApiUsage
import com.jetbrains.pluginverifier.usages.javaPlugin.JavaPluginApiUsageRegistrar
import com.jetbrains.pluginverifier.usages.javaPlugin.JavaPluginClassUsage
import com.jetbrains.pluginverifier.usages.nonExtendable.NonExtendableApiRegistrar
import com.jetbrains.pluginverifier.usages.nonExtendable.NonExtendableApiUsage
import com.jetbrains.pluginverifier.usages.overrideOnly.OverrideOnlyMethodUsage
import com.jetbrains.pluginverifier.usages.overrideOnly.OverrideOnlyRegistrar
import com.jetbrains.pluginverifier.verifiers.packages.PackageFilter

data class PluginVerificationContext(
    val idePlugin: IdePlugin,
    val verificationTarget: VerificationTarget,
    val verificationResult: VerificationResult,
    val ignoredProblems: IgnoredProblemsHolder,
    val problemFilters: List<ProblemsFilter>,
    override val externalClassesPackageFilter: PackageFilter,
    override val classResolver: Resolver,
    override val apiUsageProcessors: List<ApiUsageProcessor>
) : VerificationContext,
    ProblemRegistrar,
    DeprecatedApiRegistrar,
    ExperimentalApiRegistrar,
    OverrideOnlyRegistrar,
    InternalApiRegistrar,
    NonExtendableApiRegistrar,
    JavaPluginApiUsageRegistrar {
  override val problemRegistrar
    get() = this

  override val allProblems
    get() = verificationResult.compatibilityProblems

  override fun registerProblem(problem: CompatibilityProblem) {
    val shouldReportDecisions = problemFilters.map { it.shouldReportProblem(problem, this) }
    val ignoreDecisions = shouldReportDecisions.filterIsInstance<ProblemsFilter.Result.Ignore>()
    if (ignoreDecisions.isNotEmpty() && problem !in verificationResult.compatibilityProblems) {
      ignoredProblems.registerIgnoredProblem(problem, ignoreDecisions)
    } else if (!ignoredProblems.isIgnored(problem)) {
      verificationResult.compatibilityProblems += problem
    }
  }

  override fun unregisterProblem(problem: CompatibilityProblem) {
    verificationResult.compatibilityProblems -= problem
  }

  override fun registerDeprecatedUsage(deprecatedApiUsage: DeprecatedApiUsage) {
    val deprecatedElementHost = deprecatedApiUsage.apiElement.getHostClass()
    val usageHostClass = deprecatedApiUsage.usageLocation.getHostClass()
    if (deprecatedApiUsage is DiscouragingJdkClassUsage || shouldIndexDeprecatedClass(usageHostClass, deprecatedElementHost)) {
      verificationResult.deprecatedUsages += deprecatedApiUsage
    }
  }

  override fun registerExperimentalApiUsage(experimentalApiUsage: ExperimentalApiUsage) {
    val elementHostClass = experimentalApiUsage.apiElement.getHostClass()
    val usageHostClass = experimentalApiUsage.usageLocation.getHostClass()
    if (shouldIndexDeprecatedClass(usageHostClass, elementHostClass)) {
      verificationResult.experimentalApiUsages += experimentalApiUsage
    }
  }

  override fun registerInternalApiUsage(internalApiUsage: InternalApiUsage) {
    verificationResult.internalApiUsages += internalApiUsage
  }

  override fun registerNonExtendableApiUsage(nonExtendableApiUsage: NonExtendableApiUsage) {
    verificationResult.nonExtendableApiUsages += nonExtendableApiUsage
  }

  override fun registerOverrideOnlyMethodUsage(overrideOnlyMethodUsage: OverrideOnlyMethodUsage) {
    verificationResult.overrideOnlyMethodUsages += overrideOnlyMethodUsage
  }

  override fun registerJavaPluginClassUsage(classUsage: JavaPluginClassUsage) {
    if (idePlugin.dependencies.none { it.id == "com.intellij.modules.java" || it.id == "com.intellij.java" }) {
      verificationResult.compatibilityWarnings += NoExplicitDependencyOnJavaPluginWarning(classUsage)
    }
  }

  private fun shouldIndexDeprecatedClass(usageHostClass: ClassLocation, apiHostClass: ClassLocation): Boolean {
    val usageHostOrigin = usageHostClass.classFileOrigin
    if (idePlugin == usageHostOrigin.findOriginOfType<PluginClassFileOrigin>()?.idePlugin) {
      val apiHostOrigin = apiHostClass.classFileOrigin
      if (apiHostOrigin.isOriginOfType<IdeClassFileOrigin>()) {
        return true
      }
      val pluginOrigin = apiHostOrigin.findOriginOfType<PluginClassFileOrigin>()
      if (pluginOrigin != null && pluginOrigin.idePlugin != idePlugin) {
        return true
      }
    }
    return false
  }

  private fun Location.getHostClass() = when (this) {
    is ClassLocation -> this
    is MethodLocation -> this.hostClass
    is FieldLocation -> this.hostClass
  }

  override fun toString() = "Verification context for $idePlugin against $verificationTarget"

}