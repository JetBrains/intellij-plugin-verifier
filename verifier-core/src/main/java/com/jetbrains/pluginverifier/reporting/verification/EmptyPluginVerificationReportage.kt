package com.jetbrains.pluginverifier.reporting.verification

import com.jetbrains.pluginverifier.VerificationTarget
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.misc.impossible
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.results.VerificationResult
import com.jetbrains.pluginverifier.results.deprecated.DeprecatedApiUsage
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.results.structure.PluginStructureError
import com.jetbrains.pluginverifier.results.structure.PluginStructureWarning

object EmptyPluginVerificationReportage : PluginVerificationReportage {
  override val plugin: PluginInfo
    get() = impossible()

  override val verificationTarget: VerificationTarget
    get() = impossible()

  override fun logVerificationStarted() = Unit

  override fun logVerificationFinished(message: String) = Unit

  override fun logDependencyGraph(dependenciesGraph: DependenciesGraph) = Unit

  override fun logNewProblemDetected(problem: CompatibilityProblem) = Unit

  override fun logException(message: String, exception: Throwable) = Unit

  override fun logNewPluginStructureWarning(pluginStructureWarning: PluginStructureWarning) = Unit

  override fun logNewPluginStructureError(pluginStructureError: PluginStructureError) = Unit

  override fun logProgress(completed: Double) = Unit

  override fun logVerificationResult(verificationResult: VerificationResult) = Unit

  override fun logProblemIgnored(problem: CompatibilityProblem, reason: String) = Unit

  override fun logDeprecatedUsage(deprecatedApiUsage: DeprecatedApiUsage) = Unit

  override fun close() = Unit
}