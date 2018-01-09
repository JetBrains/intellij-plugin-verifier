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
 * Allows to save the plugin verification stages and results
 * in a configurable way.
 *
 * For instance, it is possible to configure the [PluginVerificationReportage]
 * using a [VerificationReporterSet] in such a way that
 * the results of each plugin verification would be saved to a dedicated directory.
 *
 * This interface extends [Closeable] to indicate that there could be
 * resources allocated, such as opens file streams.
 * Thus, the [PluginVerificationReportage] must be closed after the usage.
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