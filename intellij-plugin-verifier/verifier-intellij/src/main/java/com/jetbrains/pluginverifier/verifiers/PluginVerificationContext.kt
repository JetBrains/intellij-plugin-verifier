/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.verifiers

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.classes.resolvers.findOriginOfType
import com.jetbrains.plugin.structure.classes.resolvers.isOriginOfType
import com.jetbrains.plugin.structure.ide.classes.IdeFileOrigin
import com.jetbrains.plugin.structure.intellij.classes.locator.PluginFileOrigin
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.pluginverifier.PluginVerificationDescriptor
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.usages.ApiUsageProcessor
import com.jetbrains.pluginverifier.usages.deprecated.DeprecatedApiRegistrar
import com.jetbrains.pluginverifier.usages.deprecated.DeprecatedApiUsage
import com.jetbrains.pluginverifier.usages.deprecated.DeprecatedApiUsageProcessor
import com.jetbrains.pluginverifier.usages.discouraging.DiscouragingClassUsageProcessor
import com.jetbrains.pluginverifier.usages.discouraging.DiscouragingJdkClassUsage
import com.jetbrains.pluginverifier.usages.experimental.ExperimentalApiRegistrar
import com.jetbrains.pluginverifier.usages.experimental.ExperimentalApiUsage
import com.jetbrains.pluginverifier.usages.experimental.ExperimentalApiUsageProcessor
import com.jetbrains.pluginverifier.usages.internal.InternalApiUsage
import com.jetbrains.pluginverifier.usages.internal.InternalApiUsageProcessor
import com.jetbrains.pluginverifier.usages.internal.InternalApiUsageRegistrar
import com.jetbrains.pluginverifier.usages.javaPlugin.JavaPluginApiCompatibilityIssueAnalyzer
import com.jetbrains.pluginverifier.usages.javaPlugin.JavaPluginApiUsageProcessor
import com.jetbrains.pluginverifier.usages.javaPlugin.JavaPluginApiUsageRegistrar
import com.jetbrains.pluginverifier.usages.javaPlugin.JavaPluginClassUsage
import com.jetbrains.pluginverifier.usages.nonExtendable.NonExtendableApiRegistrar
import com.jetbrains.pluginverifier.usages.nonExtendable.NonExtendableApiUsage
import com.jetbrains.pluginverifier.usages.overrideOnly.OverrideOnlyMethodUsage
import com.jetbrains.pluginverifier.usages.overrideOnly.OverrideOnlyMethodUsageProcessor
import com.jetbrains.pluginverifier.usages.overrideOnly.OverrideOnlyRegistrar
import com.jetbrains.pluginverifier.usages.properties.PropertyUsageProcessor
import com.jetbrains.pluginverifier.verifiers.packages.PackageFilter
import com.jetbrains.pluginverifier.warnings.CompatibilityWarning
import com.jetbrains.pluginverifier.warnings.PluginStructureWarning
import com.jetbrains.pluginverifier.warnings.WarningRegistrar

data class PluginVerificationContext(
  val idePlugin: IdePlugin,
  val verificationDescriptor: PluginVerificationDescriptor,
  val pluginResolver: Resolver,
  val allResolver: Resolver,
  override val externalClassesPackageFilter: PackageFilter,
  val dependenciesGraph: DependenciesGraph
) : VerificationContext,
  ProblemRegistrar,
  WarningRegistrar,
  DeprecatedApiRegistrar,
  ExperimentalApiRegistrar,
  OverrideOnlyRegistrar,
  InternalApiUsageRegistrar,
  NonExtendableApiRegistrar,
  JavaPluginApiUsageRegistrar {

  override val classResolver
    get() = allResolver

  override val apiUsageProcessors: List<ApiUsageProcessor> =
    listOf(
      DeprecatedApiUsageProcessor(this),
      ExperimentalApiUsageProcessor(this),
      DiscouragingClassUsageProcessor(this),
      InternalApiUsageProcessor(this),
      OverrideOnlyMethodUsageProcessor(this),
      JavaPluginApiUsageProcessor(this),
      PropertyUsageProcessor()
    )

  private val compatibilityIssueAnalyzers = hashSetOf<CompatibilityIssueAnalyzer<*>>(JavaPluginApiCompatibilityIssueAnalyzer())

  val compatibilityProblems = hashSetOf<CompatibilityProblem>()
  val compatibilityWarnings = hashSetOf<CompatibilityWarning>()
  val deprecatedUsages = hashSetOf<DeprecatedApiUsage>()
  val experimentalApiUsages = hashSetOf<ExperimentalApiUsage>()
  val internalApiUsages = hashSetOf<InternalApiUsage>()
  val nonExtendableApiUsages = hashSetOf<NonExtendableApiUsage>()
  val overrideOnlyMethodUsages = hashSetOf<OverrideOnlyMethodUsage>()
  val pluginStructureWarnings = hashSetOf<PluginStructureWarning>()

  override val problemRegistrar
    get() = this

  override val warningRegistrar
    get() = this

  override fun registerProblem(problem: CompatibilityProblem) {
    compatibilityProblems += problem
  }

  override fun registerDeprecatedUsage(deprecatedApiUsage: DeprecatedApiUsage) {
    val deprecatedElementHost = deprecatedApiUsage.apiElement.containingClass
    val usageHostClass = deprecatedApiUsage.usageLocation.containingClass
    if (deprecatedApiUsage is DiscouragingJdkClassUsage || shouldIndexDeprecatedClass(usageHostClass, deprecatedElementHost)) {
      deprecatedUsages += deprecatedApiUsage
    }
  }

  override fun registerExperimentalApiUsage(experimentalApiUsage: ExperimentalApiUsage) {
    val elementHostClass = experimentalApiUsage.apiElement.containingClass
    val usageHostClass = experimentalApiUsage.usageLocation.containingClass
    if (shouldIndexDeprecatedClass(usageHostClass, elementHostClass)) {
      experimentalApiUsages += experimentalApiUsage
    }
  }

  override fun registerInternalApiUsage(internalApiUsage: InternalApiUsage) {
    internalApiUsages += internalApiUsage
  }

  override fun registerNonExtendableApiUsage(nonExtendableApiUsage: NonExtendableApiUsage) {
    nonExtendableApiUsages += nonExtendableApiUsage
  }

  override fun registerOverrideOnlyMethodUsage(overrideOnlyMethodUsage: OverrideOnlyMethodUsage) {
    overrideOnlyMethodUsages += overrideOnlyMethodUsage
  }

  override fun registerJavaPluginClassUsage(javaPluginClassUsage: JavaPluginClassUsage) {
    compatibilityIssueAnalyzers.filterIsInstance<JavaPluginApiCompatibilityIssueAnalyzer>()
            .map { it.analyze(this, javaPluginClassUsage) }
  }

  override fun registerCompatibilityWarning(warning: CompatibilityWarning) {
    compatibilityWarnings += warning
  }

  fun registerPluginStructureWarning(warning: PluginStructureWarning) {
    pluginStructureWarnings += warning
  }

  private fun shouldIndexDeprecatedClass(usageHostClass: ClassLocation, apiHostClass: ClassLocation): Boolean {
    val usageHostOrigin = usageHostClass.classFileOrigin
    if (idePlugin == usageHostOrigin.findOriginOfType<PluginFileOrigin>()?.idePlugin) {
      val apiHostOrigin = apiHostClass.classFileOrigin
      if (apiHostOrigin.isOriginOfType<IdeFileOrigin>()) {
        return true
      }
      val pluginOrigin = apiHostOrigin.findOriginOfType<PluginFileOrigin>()
      if (pluginOrigin != null && pluginOrigin.idePlugin != idePlugin) {
        return true
      }
    }
    return false
  }
}