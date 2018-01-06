package com.jetbrains.pluginverifier.reporting.verification

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.results.Verdict
import com.jetbrains.pluginverifier.results.deprecated.DeprecatedApiUsage
import com.jetbrains.pluginverifier.results.problems.Problem
import com.jetbrains.pluginverifier.results.warnings.Warning
import java.io.Closeable

/**
 * @author Sergey Patrikeev
 */
interface PluginVerificationReportage : Closeable {
  val plugin: PluginInfo

  val ideVersion: IdeVersion

  fun logVerificationStarted()

  fun logVerificationFinished()

  fun logDependencyGraph(dependenciesGraph: DependenciesGraph)

  fun logNewProblemDetected(problem: Problem)

  fun logNewWarningDetected(warning: Warning)

  fun logProgress(completed: Double)

  fun logVerdict(verdict: Verdict)

  fun logProblemIgnored(problem: Problem, reason: String)

  fun logDeprecatedUsage(deprecatedApiUsage: DeprecatedApiUsage)

}