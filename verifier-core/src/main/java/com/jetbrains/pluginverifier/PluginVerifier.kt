package com.jetbrains.pluginverifier

import com.jetbrains.plugin.structure.classes.resolvers.UnionResolver
import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesLocations
import com.jetbrains.pluginverifier.analysis.analyzeMissingClasses
import com.jetbrains.pluginverifier.misc.checkIfInterrupted
import com.jetbrains.pluginverifier.misc.closeLogged
import com.jetbrains.pluginverifier.parameters.classes.ExternalBuildClassesSelector
import com.jetbrains.pluginverifier.parameters.classes.MainClassesSelector
import com.jetbrains.pluginverifier.parameters.filtering.ProblemsFilter
import com.jetbrains.pluginverifier.plugin.PluginDetails
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.plugin.UnableToReadPluginClassFilesProblem
import com.jetbrains.pluginverifier.reporting.ignoring.ProblemIgnoredEvent
import com.jetbrains.pluginverifier.reporting.verification.Reportage
import com.jetbrains.pluginverifier.reporting.verification.Reporters
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.results.VerificationResult
import com.jetbrains.pluginverifier.verifiers.BytecodeVerifier
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.resolution.ClsResolverProvider
import java.util.concurrent.Callable

/**
 * Callable that performs verification
 * of [plugin] against [verificationTarget]
 * and returns [VerificationResult].
 */
class PluginVerifier(
    private val plugin: PluginInfo,
    private val reportage: Reportage,
    private val problemFilters: List<ProblemsFilter>,
    private val findDeprecatedApiUsages: Boolean,
    private val pluginDetailsCache: PluginDetailsCache,
    private val clsResolverProvider: ClsResolverProvider,
    private val verificationTarget: VerificationTarget
) : Callable<VerificationResult> {

  private val resultHolder = ResultHolder()

  override fun call(): VerificationResult {
    checkIfInterrupted()
    val pluginReporters = reportage.createPluginReporters(plugin, verificationTarget)
    val startTime = System.currentTimeMillis()
    try {
      pluginReporters.reportMessage("Start verification of $verificationTarget against $plugin")
      try {
        verify(pluginReporters)
      } catch (ie: InterruptedException) {
        throw ie
      } catch (e: Exception) {
        //[PluginVerifier] must not throw any exceptions other than [InterruptedException]
        pluginReporters.reportMessage("Failed with exception: ${e.message}")
        throw RuntimeException("Failed to verify $plugin against $verificationTarget", e)
      }

      resultHolder.reportResults(pluginReporters)

      val verificationResult = resultHolder.convertToVerificationResult()
      pluginReporters.reportVerificationResult(verificationResult)

      val elapsedTime = System.currentTimeMillis() - startTime
      pluginReporters.reportMessage("Finished verification of $verificationTarget against $plugin " +
          "in ${"%.2f".format(elapsedTime / 1000.0)} seconds: " + verificationResult.toString())
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
      failedToDownloadReason = resultHolder.failedToDownloadReason.orEmpty()
      notFoundReason = resultHolder.notFoundReason.orEmpty()
      deprecatedUsages = resultHolder.deprecatedUsages
    }
  }

  private fun verify(pluginReporters: Reporters) {
    pluginDetailsCache.getPluginDetailsCacheEntry(plugin).use {
      when (it) {
        is PluginDetailsCache.Result.Provided -> {
          val pluginDetails = it.pluginDetails
          pluginDetails.pluginWarnings.forEach { resultHolder.registerPluginErrorOrWarning(it) }
          verify(pluginDetails, pluginReporters)
        }
        is PluginDetailsCache.Result.InvalidPlugin -> it.pluginErrors.forEach { resultHolder.registerPluginErrorOrWarning(it) }
        is PluginDetailsCache.Result.FileNotFound -> resultHolder.notFoundReason = it.reason
        is PluginDetailsCache.Result.Failed -> resultHolder.failedToDownloadReason = "Plugin $plugin was not downloaded due to ${it.error.message}"
      }
    }
  }

  private fun verify(pluginDetails: PluginDetails, reporters: Reporters) {
    /**
     * Select classes for verification
     */
    val checkClasses = try {
      pluginDetails.pluginClassesLocations.getClassesForCheck()
    } catch (ie: InterruptedException) {
      throw ie
    } catch (e: Exception) {
      reporters.reportException("Failed to select classes for check for $plugin", e)
      resultHolder.registerPluginErrorOrWarning(UnableToReadPluginClassFilesProblem())
      return
    }

    clsResolverProvider.provide(pluginDetails, resultHolder).use { clsResolver ->
      val verificationContext = VerificationContext(
          plugin,
          verificationTarget,
          resultHolder,
          findDeprecatedApiUsages,
          problemFilters,
          clsResolver
      )
      BytecodeVerifier().verify(checkClasses, verificationContext) {
        reporters.reportProgress(it)
      }
      verificationContext.analyzeMissingClasses(resultHolder)
    }
  }

}

/**
 * Selectors of classes that constitute the plugin
 * class loader and of classes that should be verified.
 */
private val classesSelectors = listOf(MainClassesSelector(), ExternalBuildClassesSelector())

fun IdePluginClassesLocations.getClassesForCheck() =
    classesSelectors.flatMapTo(hashSetOf()) { it.getClassesForCheck(this) }

fun IdePluginClassesLocations.createPluginResolver() =
    UnionResolver.create(classesSelectors.map { it.getClassLoader(this) })