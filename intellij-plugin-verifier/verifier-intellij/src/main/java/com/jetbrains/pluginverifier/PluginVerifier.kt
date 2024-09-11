/*
 * Copyright 2000-2023 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier

import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.base.telemetry.MutablePluginTelemetry
import com.jetbrains.plugin.structure.base.telemetry.PLUGIN_VERIFIED_CLASSES_COUNT
import com.jetbrains.plugin.structure.classes.resolvers.CompositeResolver
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.ide.util.KnownIdePackages
import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesLocations
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.pluginverifier.analysis.ReachabilityGraph
import com.jetbrains.pluginverifier.analysis.buildClassReachabilityGraph
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.dymamic.DynamicPlugins
import com.jetbrains.pluginverifier.filtering.ApiUsageFilter
import com.jetbrains.pluginverifier.filtering.ExternalBuildClassesSelector
import com.jetbrains.pluginverifier.filtering.MainClassesSelector
import com.jetbrains.pluginverifier.filtering.ProblemsFilter
import com.jetbrains.pluginverifier.plugin.PluginDetails
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.results.problems.ClassNotFoundProblem
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.results.problems.KotlinCompatibilityModeProblemResolver
import com.jetbrains.pluginverifier.results.problems.PackageNotFoundProblem
import com.jetbrains.pluginverifier.usages.deprecated.DeprecatedMethodOverridingProcessor
import com.jetbrains.pluginverifier.usages.experimental.ExperimentalMethodOverridingProcessor
import com.jetbrains.pluginverifier.usages.internal.InternalApiUsage
import com.jetbrains.pluginverifier.usages.internal.InternalMethodOverridingProcessor
import com.jetbrains.pluginverifier.usages.nonExtendable.NonExtendableMethodOverridingProcessor
import com.jetbrains.pluginverifier.usages.nonExtendable.NonExtendableTypeInheritedProcessor
import com.jetbrains.pluginverifier.verifiers.BytecodeVerifier
import com.jetbrains.pluginverifier.verifiers.PluginVerificationContext
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.filter.ClassFilter
import com.jetbrains.pluginverifier.verifiers.method.MethodOverridingVerifier
import com.jetbrains.pluginverifier.warnings.DependenciesCycleWarning
import com.jetbrains.pluginverifier.warnings.MistakenlyBundledIdePackagesWarning
import com.jetbrains.pluginverifier.warnings.PluginStructureError
import com.jetbrains.pluginverifier.warnings.PluginStructureWarning

/**
 * Performs verification specified by [verificationDescriptor] and returns [PluginVerificationResult].
 */
class PluginVerifier(
  val verificationDescriptor: PluginVerificationDescriptor,
  private val problemFilters: List<ProblemsFilter>,
  private val pluginDetailsCache: PluginDetailsCache,
  private val classFilters: List<ClassFilter>,
  private val excludeExternalBuildClassesSelector: Boolean,
  private val apiUsageFilters: List<ApiUsageFilter> = emptyList(),
) {

  private val structureProblemsResolver = KotlinCompatibilityModeProblemResolver()

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


  fun verify(pluginDetails: PluginDetails): PluginVerificationResult {
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
      context.findMistakenlyBundledIdeClasses(pluginResolver)
      context.findDependenciesCycles(dependenciesGraph)

      val classesToCheck = selectClassesForCheck(pluginDetails).also {
        it.reportTelemetry(pluginDetails, context)
      }

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

      analyzeMissingClassesCausedByMissingOptionalDependencies(
        context.compatibilityProblems,
        dependenciesGraph,
        context.idePlugin,
        context.pluginResolver
      )
      groupMissingClassesToMissingPackages(context.compatibilityProblems, context.classResolver)

      val compatibilityProblems = context.compatibilityProblems + structureProblemsResolver.resolveCompatibilityProblems(context)
      val (reportProblems, ignoredProblems) = partitionReportAndIgnoredProblems(compatibilityProblems, context)

      val (reportedInternalApiUsages, ignoredInternalApiUsages) = partitionReportAndIgnoredInternalApiUsages(context.internalApiUsages, context)

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
          reportedInternalApiUsages,
          ignoredInternalApiUsages,
          nonExtendableApiUsages,
          overrideOnlyMethodUsages,
          pluginStructureWarnings,
          DynamicPlugins.getDynamicPluginStatus(this),
          context.telemetry
        )
      }
    }
  }

  private fun partitionReportAndIgnoredProblems(
    allProblems: Set<CompatibilityProblem>,
    verificationContext: VerificationContext
  ): Pair<MutableSet<CompatibilityProblem>, Map<CompatibilityProblem, String>> {

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

  private fun partitionReportAndIgnoredInternalApiUsages(allInternalApiUsages: Set<InternalApiUsage>, context: PluginVerificationContext): Pair<Set<InternalApiUsage>, Map<InternalApiUsage, String>> {
    val reportedUsages = mutableSetOf<InternalApiUsage>()
    val ignoredUsages = mutableMapOf<InternalApiUsage, String>()
    for (usage in allInternalApiUsages) {
      if (apiUsageFilters.isEmpty()) {
        reportedUsages += usage
      } else {
        for (apiUsageFilter in apiUsageFilters) {
          val shouldReport = apiUsageFilter.shouldReport(usage, context)
          if (shouldReport is ApiUsageFilter.Result.Ignore) {
            ignoredUsages[usage] = shouldReport.reason
            break
          } else {
            reportedUsages += usage
          }
        }
      }
    }
    return reportedUsages to ignoredUsages
  }



  /**
   * Returns the top-most package of the given [className] that is not available in this [Resolver].
   *
   * If all packages of the specified class exist, `null` is returned.
   * If the class has default (empty) package, and that default package
   * is not available, then "" is returned.
   */
  private fun Resolver.getTopMostMissingPackage(className: String): String? {
    if ('/' !in className) {
      return if (containsPackage("")) {
        null
      } else {
        ""
      }
    }
    val packageParts = className.substringBeforeLast('/', "").split('/')
    var superPackage = ""
    for (packagePart in packageParts) {
      if (superPackage.isNotEmpty()) {
        superPackage += '/'
      }
      superPackage += packagePart
      if (!containsPackage(superPackage)) {
        return superPackage
      }
    }
    return null
  }

  private fun analyzeMissingClassesCausedByMissingOptionalDependencies(
    compatibilityProblems: MutableSet<CompatibilityProblem>,
    dependenciesGraph: DependenciesGraph,
    idePlugin: IdePlugin,
    pluginResolver: Resolver
  ) {
    val classNotFoundProblems = compatibilityProblems.filterIsInstance<ClassNotFoundProblem>()
    if (classNotFoundProblems.isEmpty()) {
      return
    }

    if (dependenciesGraph.getDirectMissingDependencies().none { it.dependency.isOptional }) {
      return
    }

    val reachabilityGraph = buildClassReachabilityGraph(idePlugin, pluginResolver, dependenciesGraph)

    val ignoredProblems = arrayListOf<ClassNotFoundProblem>()
    for (classNotFoundProblem in classNotFoundProblems) {
      val usageClassName = classNotFoundProblem.usage.containingClass.className
      if (reachabilityGraph.isClassReachableFromMark(usageClassName, ReachabilityGraph.ReachabilityMark.OPTIONAL_PLUGIN)
        && !reachabilityGraph.isClassReachableFromMark(usageClassName, ReachabilityGraph.ReachabilityMark.MAIN_PLUGIN)
      ) {
        ignoredProblems += classNotFoundProblem
      }
    }

    compatibilityProblems.removeAll(ignoredProblems)
  }

  /**
   * Post-processes the verification result and groups many [ClassNotFoundProblem]s into [PackageNotFoundProblem]s,
   * to make the report easier to understand.
   */
  private fun groupMissingClassesToMissingPackages(compatibilityProblems: MutableSet<CompatibilityProblem>, classResolver: Resolver) {
    val classNotFoundProblems = compatibilityProblems.filterIsInstance<ClassNotFoundProblem>()

    /**
     * All [ClassNotFoundProblem]s will be split into 2 parts:
     * 1) Independent [ClassNotFoundProblem]s for classes
     * that originate from found packages.
     * These classes seem to be removed, causing API breakages.
     *
     * 2) Grouped [PackageNotFoundProblem]s for several [ClassNotFoundProblem]s
     * for packages that are not found.
     * These missing packages might have been removed,
     * or the Verifier is not properly configured to find them.
     */
    val noClassProblems = hashSetOf<ClassNotFoundProblem>()
    val packageToMissingProblems = hashMapOf<String, MutableSet<ClassNotFoundProblem>>()

    for (classNotFoundProblem in classNotFoundProblems) {
      val className = classNotFoundProblem.unresolved.className
      val missingPackage = classResolver.getTopMostMissingPackage(className)
      if (missingPackage != null) {
        packageToMissingProblems
          .getOrPut(missingPackage) { hashSetOf() }
          .add(classNotFoundProblem)
      } else {
        noClassProblems.add(classNotFoundProblem)
      }
    }

    // Kotlin package are supposed to be part of the platform
    val packageNotFoundProblems = packageToMissingProblems
      .map { (packageName, missingClasses) ->
        PackageNotFoundProblem(packageName, missingClasses)
      }

    //Retain all individual [ClassNotFoundProblem]s.
    for (problem in classNotFoundProblems) {
      if (problem !in noClassProblems) {
        compatibilityProblems -= problem
      }
    }

    for (packageNotFoundProblem in packageNotFoundProblems) {
      compatibilityProblems += packageNotFoundProblem
    }
  }

  private fun PluginVerificationContext.findDependenciesCycles(dependenciesGraph: DependenciesGraph) {
    val cycles = dependenciesGraph.getAllCycles()
    for (cycle in cycles) {
      registerCompatibilityWarning(DependenciesCycleWarning(cycle))
    }
  }

  private fun PluginVerificationContext.findMistakenlyBundledIdeClasses(pluginResolver: Resolver) {
    val idePackages = pluginResolver.allPackages.filter { KnownIdePackages.isKnownPackage(it.replace('/', '.')) }
    if (idePackages.isNotEmpty()) {
      registerCompatibilityWarning(MistakenlyBundledIdePackagesWarning(idePackages.map { it.replace('/', '.') }))
    }
  }

  private fun selectClassesForCheck(pluginDetails: PluginDetails): Set<String> {
    val classesForCheck = hashSetOf<String>()
    val selectorsToUse =
      if (excludeExternalBuildClassesSelector) classesSelectors.filterNot { it is ExternalBuildClassesSelector } else classesSelectors
    for (classesSelector in selectorsToUse) {
      classesForCheck += classesSelector.getClassesForCheck(pluginDetails.pluginClassesLocations)
    }
    return classesForCheck
  }

  private fun Set<String>.reportTelemetry(pluginDetails: PluginDetails, context: PluginVerificationContext) {
    context.reportTelemetry(pluginDetails.pluginInfo, MutablePluginTelemetry().apply {
      set(PLUGIN_VERIFIED_CLASSES_COUNT, this@reportTelemetry.size)
    })
  }
}

/**
 * Selectors of classes that constitute the plugin
 * class loader and of classes that should be verified.
 */
private val classesSelectors = listOf(MainClassesSelector.forPlugin(), ExternalBuildClassesSelector())

fun IdePluginClassesLocations.createPluginResolver() =
  CompositeResolver.create(classesSelectors.flatMap { it.getClassLoader(this) })