package com.jetbrains.pluginverifier

import com.jetbrains.plugin.structure.classes.resolvers.UnionResolver
import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesLocations
import com.jetbrains.pluginverifier.misc.checkIfInterrupted
import com.jetbrains.pluginverifier.parameters.classes.ExternalBuildClassesSelector
import com.jetbrains.pluginverifier.parameters.classes.MainClassesSelector
import com.jetbrains.pluginverifier.parameters.filtering.ProblemsFilter
import com.jetbrains.pluginverifier.plugin.PluginDetails
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.plugin.UnableToReadPluginClassFilesProblem
import com.jetbrains.pluginverifier.reporting.verification.VerificationReportage
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.results.VerificationResult
import com.jetbrains.pluginverifier.verifiers.BytecodeVerifier
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.resolution.ClsResolverProvider
import java.util.concurrent.Callable

class PluginVerifier(private val plugin: PluginInfo,
                     verificationReportage: VerificationReportage,
                     private val problemFilters: List<ProblemsFilter>,
                     private val findDeprecatedApiUsages: Boolean,
                     private val pluginDetailsCache: PluginDetailsCache,
                     private val clsResolverProvider: ClsResolverProvider,
                     private val verificationTarget: VerificationTarget) : Callable<VerificationResult> {

  private val pluginReportage = verificationReportage.createPluginReportage(plugin, verificationTarget)

  private val resultHolder = ResultHolder(pluginReportage)

  override fun call(): VerificationResult {
    try {
      checkIfInterrupted()
      pluginReportage.logVerificationStarted()
      verify()
    } catch (ie: InterruptedException) {
      pluginReportage.logVerificationFinished("Cancelled")
      throw ie
    } catch (e: Throwable) {
      pluginReportage.logVerificationFinished("Failed with exception: ${e.message}")
      throw RuntimeException("Failed to verify $plugin against $verificationTarget", e)
    }

    val verificationResult = resultHolder.convertToVerificationResult()
    pluginReportage.logVerificationResult(verificationResult)
    pluginReportage.logVerificationFinished(verificationResult.toString())
    return verificationResult
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
      ignoredProblems = resultHolder.ignoredProblemsHolder.ignoredProblems
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

  private fun verify() {
    pluginDetailsCache.getPluginDetailsCacheEntry(plugin).use {
      when (it) {
        is PluginDetailsCache.Result.Provided -> {
          val pluginDetails = it.pluginDetails
          pluginDetails.pluginWarnings.forEach { resultHolder.registerPluginErrorOrWarning(it) }
          verify(pluginDetails)
        }
        is PluginDetailsCache.Result.InvalidPlugin -> it.pluginErrors.forEach { resultHolder.registerPluginErrorOrWarning(it) }
        is PluginDetailsCache.Result.FileNotFound -> resultHolder.notFoundReason = it.reason
        is PluginDetailsCache.Result.Failed -> resultHolder.failedToDownloadReason = "Plugin $plugin was not downloaded due to ${it.error.message}"
      }
    }
  }

  private fun verify(pluginDetails: PluginDetails) {
    /**
     * Select classes for verification
     */
    val checkClasses = try {
      pluginDetails.pluginClassesLocations.getClassesForCheck()
    } catch (e: Exception) {
      pluginReportage.logException("Failed to select classes for check for $plugin", e)
      resultHolder.registerPluginErrorOrWarning(UnableToReadPluginClassFilesProblem())
      return
    }

    clsResolverProvider.provide(pluginDetails, resultHolder, pluginReportage).use { clsResolver ->
      val verificationContext = VerificationContext(
          plugin,
          verificationTarget,
          resultHolder,
          findDeprecatedApiUsages,
          problemFilters,
          clsResolver
      )
      BytecodeVerifier().verify(checkClasses, verificationContext) { pluginReportage.logProgress(it) }
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