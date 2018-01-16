package com.jetbrains.pluginverifier.reporting.verification

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.misc.impossible
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.results.VerificationResult
import com.jetbrains.pluginverifier.results.deprecated.DeprecatedApiUsage
import com.jetbrains.pluginverifier.results.problems.Problem

object EmptyPluginVerificationReportage : PluginVerificationReportage {
  override val plugin: PluginInfo
    get() = impossible()

  override val ideVersion: IdeVersion
    get() = impossible()

  override fun logVerificationStarted() = Unit

  override fun logVerificationFinished(message: String) = Unit

  override fun logDependencyGraph(dependenciesGraph: DependenciesGraph) = Unit

  override fun logNewProblemDetected(problem: Problem) = Unit

  override fun logException(message: String, exception: Throwable) = Unit

  override fun logNewWarningDetected(warning: PluginProblem) = Unit

  override fun logProgress(completed: Double) = Unit

  override fun logVerificationResult(verificationResult: VerificationResult) = Unit

  override fun logProblemIgnored(problem: Problem, reason: String) = Unit

  override fun logDeprecatedUsage(deprecatedApiUsage: DeprecatedApiUsage) = Unit

  override fun close() = Unit
}