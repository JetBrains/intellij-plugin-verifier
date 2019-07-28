package com.jetbrains.pluginverifier

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.classes.resolvers.CompositeResolver
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.ide.PluginIdAndVersion
import com.jetbrains.plugin.structure.ide.util.KnownIdePackages
import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesLocations
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.filtering.ExternalBuildClassesSelector
import com.jetbrains.pluginverifier.filtering.MainClassesSelector
import com.jetbrains.pluginverifier.filtering.ProblemsFilter
import com.jetbrains.pluginverifier.plugin.PluginDetails
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.resolution.ClassResolverProvider
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
 * Performs verification of [plugin] against [verificationTarget] and returns [PluginVerificationResult].
 */
class PluginVerifier(
    val plugin: PluginInfo,
    val verificationTarget: PluginVerificationTarget,

    private val problemFilters: List<ProblemsFilter>,
    private val pluginDetailsCache: PluginDetailsCache,
    private val classResolverProvider: ClassResolverProvider,
    private val classFilters: List<ClassFilter>
) {

  fun loadPluginAndVerify(): PluginVerificationResult {
    pluginDetailsCache.getPluginDetailsCacheEntry(plugin).use { cacheEntry ->
      return when (cacheEntry) {
        is PluginDetailsCache.Result.InvalidPlugin -> {
          PluginVerificationResult.InvalidPlugin(
              plugin,
              verificationTarget,
              cacheEntry.pluginErrors
                  .filter { it.level == PluginProblem.Level.ERROR }
                  .mapTo(hashSetOf()) { PluginStructureError(it) }
          )
        }
        is PluginDetailsCache.Result.FileNotFound -> {
          PluginVerificationResult.NotFound(plugin, verificationTarget, cacheEntry.reason)
        }
        is PluginDetailsCache.Result.Failed -> {
          PluginVerificationResult.FailedToDownload(plugin, verificationTarget, cacheEntry.reason, cacheEntry.error)
        }
        is PluginDetailsCache.Result.Provided -> {
          verify(cacheEntry.pluginDetails)
        }
      }
    }
  }


  private fun verify(pluginDetails: PluginDetails): PluginVerificationResult {
    classResolverProvider.provide(pluginDetails).use { (pluginResolver, classResolver, dependenciesGraph) ->
      val externalClassesPackageFilter = classResolverProvider.provideExternalClassesPackageFilter()

      val context = PluginVerificationContext(
          pluginDetails.idePlugin,
          verificationTarget,
          classResolver,
          externalClassesPackageFilter
      )

      pluginDetails.pluginWarnings.forEach { context.registerCompatibilityWarning(PluginStructureWarning(it)) }
      context.checkIfPluginIsMarkedIncompatibleWithThisIde(verificationTarget)
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
            plugin,
            verificationTarget,
            dependenciesGraph,
            reportProblems,
            ignoredProblems,
            compatibilityWarnings,
            deprecatedUsages,
            experimentalApiUsages,
            internalApiUsages,
            nonExtendableApiUsages,
            overrideOnlyMethodUsages
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


  private fun PluginVerificationContext.checkIfPluginIsMarkedIncompatibleWithThisIde(verificationTarget: PluginVerificationTarget) {
    if (verificationTarget is PluginVerificationTarget.IDE) {
      if (PluginIdAndVersion(plugin.pluginId, plugin.version) in verificationTarget.incompatiblePlugins) {
        registerProblem(PluginIsMarkedIncompatibleProblem(plugin, verificationTarget.ideVersion))
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