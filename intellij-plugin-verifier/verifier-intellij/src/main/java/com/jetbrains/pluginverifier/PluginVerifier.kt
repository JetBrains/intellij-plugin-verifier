package com.jetbrains.pluginverifier

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.base.utils.checkIfInterrupted
import com.jetbrains.plugin.structure.base.utils.closeLogged
import com.jetbrains.plugin.structure.base.utils.rethrowIfInterrupted
import com.jetbrains.plugin.structure.classes.resolvers.UnionResolver
import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesLocations
import com.jetbrains.pluginverifier.parameters.classes.ExternalBuildClassesSelector
import com.jetbrains.pluginverifier.parameters.classes.MainClassesSelector
import com.jetbrains.pluginverifier.parameters.filtering.IgnoredProblemsHolder
import com.jetbrains.pluginverifier.parameters.filtering.ProblemsFilter
import com.jetbrains.pluginverifier.plugin.PluginDetails
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.reporting.ignoring.ProblemIgnoredEvent
import com.jetbrains.pluginverifier.reporting.verification.Reportage
import com.jetbrains.pluginverifier.reporting.verification.Reporters
import com.jetbrains.pluginverifier.repository.PluginIdAndVersion
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.resolution.ClassResolverProvider
import com.jetbrains.pluginverifier.results.VerificationResult
import com.jetbrains.pluginverifier.results.problems.PluginIsMarkedIncompatibleProblem
import com.jetbrains.pluginverifier.results.structure.PluginStructureError
import com.jetbrains.pluginverifier.results.structure.PluginStructureWarning
import com.jetbrains.pluginverifier.usages.deprecated.DeprecatedApiUsageProcessor
import com.jetbrains.pluginverifier.usages.deprecated.DeprecatedMethodOverridingProcessor
import com.jetbrains.pluginverifier.usages.discouraging.DiscouragingClassUsageProcessor
import com.jetbrains.pluginverifier.usages.experimental.ExperimentalApiUsageProcessor
import com.jetbrains.pluginverifier.usages.experimental.ExperimentalMethodOverridingProcessor
import com.jetbrains.pluginverifier.usages.internal.InternalApiUsageProcessor
import com.jetbrains.pluginverifier.usages.internal.InternalMethodOverridingProcessor
import com.jetbrains.pluginverifier.usages.nonExtendable.NonExtendableMethodOverridingProcessor
import com.jetbrains.pluginverifier.usages.nonExtendable.NonExtendableTypeInheritedVerifier
import com.jetbrains.pluginverifier.usages.overrideOnly.OverrideOnlyMethodUsageProcessor
import com.jetbrains.pluginverifier.verifiers.BytecodeVerifier
import com.jetbrains.pluginverifier.verifiers.PluginVerificationContext
import com.jetbrains.pluginverifier.verifiers.clazz.PluginClassFileVersionVerifier
import com.jetbrains.pluginverifier.verifiers.filter.ClassFilter
import com.jetbrains.pluginverifier.verifiers.method.MethodOverridingVerifier
import com.jetbrains.pluginverifier.verifiers.resolution.ClassResolver
import java.util.concurrent.Callable

/**
 * Callable that performs verification
 * of [plugin] against [verificationTarget]
 * and returns [VerificationResult].
 */
class PluginVerifier(
    val plugin: PluginInfo,
    reportage: Reportage,
    private val problemFilters: List<ProblemsFilter>,
    private val checkApiUsages: Boolean,
    private val pluginDetailsCache: PluginDetailsCache,
    private val classResolverProvider: ClassResolverProvider,
    private val verificationTarget: VerificationTarget,
    private val incompatiblePlugins: Set<PluginIdAndVersion>,
    private val classFilters: List<ClassFilter>
) : Callable<VerificationResult> {

  private val pluginReporters = reportage.createPluginReporters(plugin, verificationTarget)

  override fun call(): VerificationResult {
    checkIfInterrupted()
    val startTime = System.currentTimeMillis()
    try {
      pluginReporters.reportMessage("Start verification of $verificationTarget against $plugin")

      val ignoredProblems = IgnoredProblemsHolder()
      val verificationResult = VerificationResult.OK()
      verificationResult.plugin = plugin
      verificationResult.verificationTarget = verificationTarget

      /**
       * Register a special "marked incompatible" problem, if necessary.
       */
      if (verificationTarget is VerificationTarget.Ide && PluginIdAndVersion(plugin.pluginId, plugin.version) in incompatiblePlugins) {
        verificationResult.compatibilityProblems += PluginIsMarkedIncompatibleProblem(plugin, verificationTarget.ideVersion)
      }

      try {
        loadPluginAndVerify(verificationResult, ignoredProblems)
      } catch (e: Exception) {
        e.rethrowIfInterrupted()
        //[PluginVerifier] must not throw any exceptions other than [InterruptedException]
        pluginReporters.reportMessage("Failed with exception: ${e.message}")
        throw RuntimeException("Failed to verify $plugin against $verificationTarget", e)
      }

      pluginReporters.reportResults(verificationResult, ignoredProblems)

      val elapsedTime = System.currentTimeMillis() - startTime
      pluginReporters.reportMessage(
          "Finished verification of $verificationTarget against $plugin " +
              "in ${"%.2f".format(elapsedTime / 1000.0)} seconds: " + verificationResult.toString()
      )
      return buildFinalResult(verificationResult)
    } finally {
      pluginReporters.closeLogged()
    }
  }

  private fun buildFinalResult(r: VerificationResult): VerificationResult {
    return when {
      r.pluginStructureErrors.isNotEmpty() -> {
        VerificationResult.InvalidPlugin()
      }
      r.notFoundReason != null -> {
        VerificationResult.NotFound()
      }
      r.failedToDownloadReason != null -> {
        VerificationResult.FailedToDownload()
      }
      r.dependenciesGraph.verifiedPlugin.missingDependencies.isNotEmpty() -> {
        VerificationResult.MissingDependencies()
      }
      r.compatibilityProblems.isNotEmpty() -> {
        VerificationResult.CompatibilityProblems()
      }
      r.pluginStructureWarnings.isNotEmpty() -> {
        VerificationResult.StructureWarnings()
      }
      else -> {
        VerificationResult.OK()
      }
    }.apply {
      plugin = r.plugin
      verificationTarget = r.verificationTarget
      pluginStructureWarnings.addAll(r.pluginStructureWarnings)
      pluginStructureErrors.addAll(r.pluginStructureErrors)
      compatibilityProblems.addAll(r.compatibilityProblems)
      deprecatedUsages.addAll(r.deprecatedUsages)
      experimentalApiUsages.addAll(r.experimentalApiUsages)
      internalApiUsages.addAll(r.internalApiUsages)
      nonExtendableApiUsages.addAll(r.nonExtendableApiUsages)
      overrideOnlyMethodUsages.addAll(r.overrideOnlyMethodUsages)
      dependenciesGraph = r.dependenciesGraph
      failedToDownloadReason = r.failedToDownloadReason
      failedToDownloadError = r.failedToDownloadError
      notFoundReason = r.notFoundReason
    }
  }

  private fun Reporters.reportResults(result: VerificationResult, ignoredProblems: IgnoredProblemsHolder) {
    reportVerificationResult(result)
    result.pluginStructureErrors.forEach { reportNewPluginStructureError(it) }
    result.pluginStructureWarnings.forEach { reportNewPluginStructureWarning(it) }
    result.compatibilityProblems.forEach { reportNewProblemDetected(it) }
    result.deprecatedUsages.forEach { reportDeprecatedUsage(it) }
    result.experimentalApiUsages.forEach { reportExperimentalApi(it) }
    for ((problem, ignoredEvents) in ignoredProblems.ignoredProblems) {
      for (ignoreEvent in ignoredEvents) {
        reportProblemIgnored(
            ProblemIgnoredEvent(result.plugin, result.verificationTarget, problem, ignoreEvent.reason)
        )
      }
    }
    reportDependencyGraph(result.dependenciesGraph)
  }

  private fun loadPluginAndVerify(verificationResult: VerificationResult, ignoredProblems: IgnoredProblemsHolder) {
    pluginDetailsCache.getPluginDetailsCacheEntry(plugin).use { cacheEntry ->
      when (cacheEntry) {
        is PluginDetailsCache.Result.Provided -> {
          val pluginDetails = cacheEntry.pluginDetails
          pluginDetails.pluginWarnings.forEach { verificationResult.addPluginErrorOrWarning(it) }
          verifyClasses(pluginDetails, verificationResult, ignoredProblems)
        }
        is PluginDetailsCache.Result.InvalidPlugin -> {
          cacheEntry.pluginErrors.forEach { verificationResult.addPluginErrorOrWarning(it) }
        }
        is PluginDetailsCache.Result.FileNotFound -> {
          verificationResult.notFoundReason = cacheEntry.reason
        }
        is PluginDetailsCache.Result.Failed -> {
          verificationResult.failedToDownloadReason = cacheEntry.reason
          verificationResult.failedToDownloadError = cacheEntry.error
        }
      }
    }
  }

  private fun VerificationResult.addPluginErrorOrWarning(pluginProblem: PluginProblem) {
    if (pluginProblem.level == PluginProblem.Level.WARNING) {
      pluginStructureWarnings += PluginStructureWarning(pluginProblem.message)
    } else {
      pluginStructureErrors += PluginStructureError(pluginProblem.message)
    }
  }

  private fun verifyClasses(
      pluginDetails: PluginDetails,
      verificationResult: VerificationResult,
      ignoredProblems: IgnoredProblemsHolder
  ) {
    val checkClasses = try {
      selectClassesForCheck(pluginDetails)
    } catch (e: Exception) {
      e.rethrowIfInterrupted()
      pluginReporters.reportException("Failed to select classes for check for $plugin", e)
      verificationResult.pluginStructureErrors += PluginStructureError(
          "Unable to read plugin class files" + (e.localizedMessage?.let { ": $it" } ?: "")
      )
      return
    }

    classResolverProvider.provide(pluginDetails, verificationResult).use { classResolver ->
      val context = createVerificationContext(classResolver, verificationResult, ignoredProblems)
      runByteCodeVerifier(checkClasses, context)
    }
  }

  private fun createVerificationContext(
      classResolver: ClassResolver,
      verificationResult: VerificationResult,
      ignoredProblems: IgnoredProblemsHolder
  ) = PluginVerificationContext(
      plugin,
      verificationTarget,
      verificationResult,
      ignoredProblems,
      checkApiUsages,
      problemFilters,
      classResolver,
      listOf(
          DeprecatedApiUsageProcessor(),
          ExperimentalApiUsageProcessor(),
          DiscouragingClassUsageProcessor(),
          InternalApiUsageProcessor(),
          OverrideOnlyMethodUsageProcessor()
      )
  )

  private fun runByteCodeVerifier(checkClasses: Set<String>, context: PluginVerificationContext) {
    BytecodeVerifier(
        classFilters,
        listOf(
            PluginClassFileVersionVerifier(),
            NonExtendableTypeInheritedVerifier()
        ),
        listOf(
            MethodOverridingVerifier(
                listOf(
                    ExperimentalMethodOverridingProcessor(),
                    DeprecatedMethodOverridingProcessor(),
                    NonExtendableMethodOverridingProcessor(),
                    InternalMethodOverridingProcessor()
                )
            )
        )
    ).verify(checkClasses, context) {
      pluginReporters.reportProgress(it)
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
    UnionResolver.create(classesSelectors.map { it.getClassLoader(this) })