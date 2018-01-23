package com.jetbrains.pluginverifier.reporting.verification

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.results.VerificationResult
import com.jetbrains.pluginverifier.results.deprecated.DeprecatedApiUsage
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.results.structure.PluginStructureError
import com.jetbrains.pluginverifier.results.structure.PluginStructureWarning
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
 * resources allocated, such as open file streams.
 * Thus, the [PluginVerificationReportage] must be closed after the usage.
 */
interface PluginVerificationReportage : Closeable {
  val plugin: PluginInfo

  val ideVersion: IdeVersion

  fun logVerificationStarted()

  fun logVerificationFinished(message: String)

  fun logException(message: String, exception: Throwable)

  fun logDependencyGraph(dependenciesGraph: DependenciesGraph)

  fun logNewProblemDetected(problem: CompatibilityProblem)

  fun logNewPluginStructureWarning(pluginStructureWarning: PluginStructureWarning)

  fun logNewPluginStructureError(pluginStructureError: PluginStructureError)

  fun logProgress(completed: Double)

  fun logVerificationResult(verificationResult: VerificationResult)

  fun logProblemIgnored(problem: CompatibilityProblem, reason: String)

  fun logDeprecatedUsage(deprecatedApiUsage: DeprecatedApiUsage)

}