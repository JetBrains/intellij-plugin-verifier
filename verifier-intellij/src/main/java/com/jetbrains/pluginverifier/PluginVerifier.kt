package com.jetbrains.pluginverifier

import com.jetbrains.plugin.structure.base.utils.checkIfInterrupted
import com.jetbrains.plugin.structure.base.utils.closeLogged
import com.jetbrains.plugin.structure.base.utils.rethrowIfInterrupted
import com.jetbrains.plugin.structure.classes.resolvers.UnionResolver
import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesLocations
import com.jetbrains.pluginverifier.parameters.classes.ExternalBuildClassesSelector
import com.jetbrains.pluginverifier.parameters.classes.MainClassesSelector
import com.jetbrains.pluginverifier.parameters.filtering.ProblemsFilter
import com.jetbrains.pluginverifier.plugin.PluginDetails
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.plugin.UnableToReadPluginClassFilesProblem
import com.jetbrains.pluginverifier.reporting.ignoring.ProblemIgnoredEvent
import com.jetbrains.pluginverifier.reporting.verification.Reportage
import com.jetbrains.pluginverifier.reporting.verification.Reporters
import com.jetbrains.pluginverifier.repository.PluginIdAndVersion
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.resolution.ClassResolverProvider
import com.jetbrains.pluginverifier.results.VerificationResult
import com.jetbrains.pluginverifier.results.problems.PluginIsMarkedIncompatibleProblem
import com.jetbrains.pluginverifier.verifiers.BytecodeVerifier
import com.jetbrains.pluginverifier.verifiers.PluginVerificationContext
import com.jetbrains.pluginverifier.verifiers.clazz.PluginClassFileVersionVerifier
import com.jetbrains.pluginverifier.verifiers.filter.ClassFilter
import com.jetbrains.pluginverifier.verifiers.method.UnstableMethodOverriddenVerifier
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
    private val findDeprecatedApiUsages: Boolean,
    private val pluginDetailsCache: PluginDetailsCache,
    private val classResolverProvider: ClassResolverProvider,
    private val verificationTarget: VerificationTarget,
    private val incompatiblePlugins: Set<PluginIdAndVersion>,
    private val classFilters: List<ClassFilter>
) : Callable<VerificationResult> {

  private val resultHolder = ResultHolder()

  private val pluginReporters = reportage.createPluginReporters(plugin, verificationTarget)

  override fun call(): VerificationResult {
    checkIfInterrupted()
    val startTime = System.currentTimeMillis()
    try {
      pluginReporters.reportMessage("Start verification of $verificationTarget against $plugin")

      /**
       * Register a special "marked incompatible" problem, if necessary.
       */
      if (verificationTarget is VerificationTarget.Ide && PluginIdAndVersion(plugin.pluginId, plugin.version) in incompatiblePlugins) {
        resultHolder.addProblem(PluginIsMarkedIncompatibleProblem(plugin, verificationTarget.ideVersion))
      }

      try {
        loadPluginAndVerify()
      } catch (e: Exception) {
        e.rethrowIfInterrupted()
        //[PluginVerifier] must not throw any exceptions other than [InterruptedException]
        pluginReporters.reportMessage("Failed with exception: ${e.message}")
        throw RuntimeException("Failed to verify $plugin against $verificationTarget", e)
      }

      resultHolder.reportResults(pluginReporters)

      val verificationResult = resultHolder.convertToVerificationResult()
      pluginReporters.reportVerificationResult(verificationResult)

      val elapsedTime = System.currentTimeMillis() - startTime
      pluginReporters.reportMessage(
          "Finished verification of $verificationTarget against $plugin " +
              "in ${"%.2f".format(elapsedTime / 1000.0)} seconds: " + verificationResult.toString()
      )
      return verificationResult
    } finally {
      pluginReporters.closeLogged()
    }
  }

  private fun ResultHolder.reportResults(pluginReportage: Reporters) {
    pluginStructureErrors.forEach { pluginReportage.reportNewPluginStructureError(it) }
    pluginStructureWarnings.forEach { pluginReportage.reportNewPluginStructureWarning(it) }
    compatibilityProblems.forEach { pluginReportage.reportNewProblemDetected(it) }
    deprecatedUsages.forEach { pluginReportage.reportDeprecatedUsage(it) }
    experimentalApiUsages.forEach { pluginReportage.reportExperimentalApi(it) }
    ignoredProblemsHolder.ignoredProblems.forEach { (problem, ignoredEvents) ->
      ignoredEvents.forEach {
        val problemIgnoredEvent = ProblemIgnoredEvent(plugin, verificationTarget, problem, it.reason)
        pluginReportage.reportProblemIgnored(problemIgnoredEvent)
      }
    }
    if (dependenciesGraph != null) {
      pluginReportage.reportDependencyGraph(dependenciesGraph!!)
    }
  }

  private fun ResultHolder.convertToVerificationResult(): VerificationResult {
    return when {
      pluginStructureErrors.isNotEmpty() -> VerificationResult.InvalidPlugin()
      notFoundReason != null -> VerificationResult.NotFound()
      failedToDownloadReason != null -> VerificationResult.FailedToDownload()
      dependenciesGraph != null && dependenciesGraph!!.verifiedPlugin.missingDependencies.isNotEmpty() -> VerificationResult.MissingDependencies()
      compatibilityProblems.isNotEmpty() -> VerificationResult.CompatibilityProblems()
      pluginStructureWarnings.isNotEmpty() -> VerificationResult.StructureWarnings()
      else -> VerificationResult.OK()
    }.apply {
      plugin = this@PluginVerifier.plugin
      verificationTarget = this@PluginVerifier.verificationTarget
      ignoredProblems = resultHolder.ignoredProblemsHolder.ignoredProblems.keys.toSet()
      if (resultHolder.dependenciesGraph != null) {
        dependenciesGraph = resultHolder.dependenciesGraph!!
      }
      pluginStructureWarnings = resultHolder.pluginStructureWarnings
      pluginStructureErrors = resultHolder.pluginStructureErrors
      compatibilityProblems = resultHolder.compatibilityProblems
      failedToDownloadReason = resultHolder.failedToDownloadReason
      failedToDownloadError = resultHolder.failedToDownloadError
      notFoundReason = resultHolder.notFoundReason
      deprecatedUsages = resultHolder.deprecatedUsages
      experimentalApiUsages = resultHolder.experimentalApiUsages
    }
  }

  private fun loadPluginAndVerify() {
    pluginDetailsCache.getPluginDetailsCacheEntry(plugin).use { cacheEntry ->
      when (cacheEntry) {
        is PluginDetailsCache.Result.Provided -> {
          val pluginDetails = cacheEntry.pluginDetails
          pluginDetails.pluginWarnings.forEach { resultHolder.addPluginErrorOrWarning(it) }
          verifyClasses(pluginDetails)
        }
        is PluginDetailsCache.Result.InvalidPlugin -> {
          cacheEntry.pluginErrors.forEach { resultHolder.addPluginErrorOrWarning(it) }
        }
        is PluginDetailsCache.Result.FileNotFound -> resultHolder.notFoundReason = cacheEntry.reason
        is PluginDetailsCache.Result.Failed -> {
          resultHolder.failedToDownloadReason = cacheEntry.reason
          resultHolder.failedToDownloadError = cacheEntry.error
        }
      }
    }
  }

  private fun verifyClasses(pluginDetails: PluginDetails) {
    val checkClasses = try {
      selectClassesForCheck(pluginDetails)
    } catch (e: Exception) {
      e.rethrowIfInterrupted()
      pluginReporters.reportException("Failed to select classes for check for $plugin", e)
      resultHolder.addPluginErrorOrWarning(UnableToReadPluginClassFilesProblem(e.localizedMessage))
      return
    }

    classResolverProvider.provide(pluginDetails, resultHolder, pluginReporters).use { classResolver ->
      val context = PluginVerificationContext(
          plugin,
          verificationTarget,
          resultHolder,
          findDeprecatedApiUsages,
          problemFilters,
          classResolver
      )
      BytecodeVerifier(
          classFilters,
          listOf(PluginClassFileVersionVerifier()),
          listOf(UnstableMethodOverriddenVerifier())
      ).verify(checkClasses, context) {
        pluginReporters.reportProgress(it)
      }
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