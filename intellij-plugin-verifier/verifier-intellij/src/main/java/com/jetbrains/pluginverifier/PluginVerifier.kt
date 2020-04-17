/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.classes.resolvers.CompositeResolver
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.ide.PluginIdAndVersion
import com.jetbrains.plugin.structure.ide.util.KnownIdePackages
import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesLocations
import com.jetbrains.plugin.structure.intellij.plugin.ExtensionPoint
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginImpl
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.dymamic.DynamicPlugins
import com.jetbrains.pluginverifier.filtering.ExternalBuildClassesSelector
import com.jetbrains.pluginverifier.filtering.MainClassesSelector
import com.jetbrains.pluginverifier.filtering.ProblemsFilter
import com.jetbrains.pluginverifier.plugin.PluginDetails
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.usages.deprecated.DeprecatedMethodOverridingProcessor
import com.jetbrains.pluginverifier.usages.experimental.ExperimentalMethodOverridingProcessor
import com.jetbrains.pluginverifier.usages.internal.InternalMethodOverridingProcessor
import com.jetbrains.pluginverifier.usages.nonExtendable.NonExtendableMethodOverridingProcessor
import com.jetbrains.pluginverifier.usages.nonExtendable.NonExtendableTypeInheritedProcessor
import com.jetbrains.pluginverifier.verifiers.BytecodeVerifier
import com.jetbrains.pluginverifier.verifiers.PluginVerificationContext
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.filter.ClassFilter
import com.jetbrains.pluginverifier.verifiers.method.MethodOverridingVerifier
import com.jetbrains.pluginverifier.warnings.*

/**
 * Performs verification specified by [verificationDescriptor] and returns [PluginVerificationResult].
 */
class PluginVerifier(
  val verificationDescriptor: PluginVerificationDescriptor,
  private val problemFilters: List<ProblemsFilter>,
  private val pluginDetailsCache: PluginDetailsCache,
  private val classFilters: List<ClassFilter>
) {

  fun loadPluginAndVerify(): PluginVerificationResult {
    pluginDetailsCache.getPluginDetailsCacheEntry(verificationDescriptor.checkedPlugin).use { cacheEntry ->
      return when (cacheEntry) {
        is PluginDetailsCache.Result.InvalidPlugin -> {
          PluginVerificationResult.InvalidPlugin(
            verificationDescriptor.checkedPlugin,
            verificationDescriptor.toTarget(),
            cacheEntry.pluginErrors
              .filter { it.level == PluginProblem.Level.ERROR }
              .mapTo(hashSetOf()) { PluginStructureError(it) }
          )
        }
        is PluginDetailsCache.Result.FileNotFound -> {
          PluginVerificationResult.NotFound(verificationDescriptor.checkedPlugin, verificationDescriptor.toTarget(), cacheEntry.reason)
        }
        is PluginDetailsCache.Result.Failed -> {
          PluginVerificationResult.FailedToDownload(verificationDescriptor.checkedPlugin, verificationDescriptor.toTarget(), cacheEntry.reason)
        }
        is PluginDetailsCache.Result.Provided -> {
          verify(cacheEntry.pluginDetails)
        }
      }
    }
  }


  private fun verify(pluginDetails: PluginDetails): PluginVerificationResult {
    verificationDescriptor.classResolverProvider.provide(pluginDetails).use { (pluginResolver, allResolver, dependenciesGraph) ->
      val externalClassesPackageFilter = verificationDescriptor.classResolverProvider.provideExternalClassesPackageFilter()

      val context = PluginVerificationContext(
        pluginDetails.idePlugin,
        verificationDescriptor,
        pluginResolver,
        allResolver,
        externalClassesPackageFilter,
        dependenciesGraph
      )

      pluginDetails.pluginWarnings.forEach { context.registerPluginStructureWarning(PluginStructureWarning(it)) }
      context.checkIfPluginIsMarkedIncompatibleWithThisIde()
      context.findMistakenlyBundledIdeClasses(pluginResolver)
      context.findDependenciesCycles(dependenciesGraph)

      val classesToCheck = selectClassesForCheck(pluginDetails)

      BytecodeVerifier(
        classFilters,
        listOf(NonExtendableTypeInheritedProcessor(context)),
        listOf(
          MethodOverridingVerifier(
            listOf(
              ExperimentalMethodOverridingProcessor(context),
              DeprecatedMethodOverridingProcessor(context),
              NonExtendableMethodOverridingProcessor(context),
              InternalMethodOverridingProcessor(context)
            )
          )
        )
      ).verify(classesToCheck, context) {}

      context.postProcessResults()

      val (reportProblems, ignoredProblems) = partitionReportAndIgnoredProblems(context.compatibilityProblems, context)

      return with(context) {
        PluginVerificationResult.Verified(
          verificationDescriptor.checkedPlugin,
          verificationDescriptor.toTarget(),
          dependenciesGraph,
          reportProblems,
          ignoredProblems,
          compatibilityWarnings,
          deprecatedUsages,
          experimentalApiUsages,
          internalApiUsages,
          nonExtendableApiUsages,
          overrideOnlyMethodUsages,
          pluginStructureWarnings,
          DynamicPlugins.getDynamicPluginStatus(this)
        )
      }
    }
  }

  private fun partitionReportAndIgnoredProblems(
    allProblems: Set<CompatibilityProblem>,
    verificationContext: VerificationContext
  ): Pair<Set<CompatibilityProblem>, Map<CompatibilityProblem, String>> {

    val reportProblems = hashSetOf<CompatibilityProblem>()
    val ignoredProblems = hashMapOf<CompatibilityProblem, String>()

    for (problem in allProblems) {
      val ignoreDecision = problemFilters.asSequence()
        .map { it.shouldReportProblem(problem, verificationContext) }
        .filterIsInstance<ProblemsFilter.Result.Ignore>()
        .firstOrNull()

      if (ignoreDecision != null) {
        ignoredProblems[problem] = ignoreDecision.reason
      } else {
        reportProblems += problem
      }
    }

    return reportProblems to ignoredProblems
  }

  private fun PluginVerificationContext.checkIfPluginIsMarkedIncompatibleWithThisIde() {
    if (verificationDescriptor is PluginVerificationDescriptor.IDE) {
      if (PluginIdAndVersion(verificationDescriptor.checkedPlugin.pluginId, verificationDescriptor.checkedPlugin.version) in verificationDescriptor.incompatiblePlugins) {
        registerProblem(PluginIsMarkedIncompatibleProblem(verificationDescriptor.checkedPlugin, verificationDescriptor.ideVersion))
      }
    }
  }

  private fun PluginVerificationContext.findDependenciesCycles(dependenciesGraph: DependenciesGraph) {
    val cycles = dependenciesGraph.getAllCycles()
    for (cycle in cycles) {
      registerCompatibilityWarning(DependenciesCycleWarning(cycle))
    }
  }

  private fun PluginVerificationContext.findMistakenlyBundledIdeClasses(pluginResolver: Resolver) {
    val idePackages = pluginResolver.allPackages.filter { KnownIdePackages.isKnownPackage(it) }
    if (idePackages.isNotEmpty()) {
      registerCompatibilityWarning(MistakenlyBundledIdePackagesWarning(idePackages))
    }
  }

  private fun selectClassesForCheck(pluginDetails: PluginDetails): Set<String> {
    val classesForCheck = hashSetOf<String>()
    for (classesSelector in classesSelectors) {
      classesForCheck += classesSelector.getClassesForCheck(pluginDetails.pluginClassesLocations)
    }
    return classesForCheck
  }

}

/**
 * Selectors of classes that constitute the plugin
 * class loader and of classes that should be verified.
 */
private val classesSelectors = listOf(MainClassesSelector(), ExternalBuildClassesSelector())

fun IdePluginClassesLocations.createPluginResolver() =
  CompositeResolver.create(classesSelectors.flatMap { it.getClassLoader(this) })