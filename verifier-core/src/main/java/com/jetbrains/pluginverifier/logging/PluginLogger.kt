package com.jetbrains.pluginverifier.logging

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.plugin.PluginCoordinate
import com.jetbrains.pluginverifier.results.Verdict
import com.jetbrains.pluginverifier.results.problems.Problem
import com.jetbrains.pluginverifier.results.warnings.Warning

/**
 * @author Sergey Patrikeev
 */
interface PluginLogger {
  val plugin: PluginCoordinate

  val ideVersion: IdeVersion

  fun logVerificationStarted()

  fun logVerificationFinished()

  fun logDependencyGraph(dependenciesGraph: DependenciesGraph)

  fun logNewProblemDetected(problem: Problem)

  fun logNewWarningDetected(warning: Warning)

  fun logCompletedClasses(fraction: Double)

  fun logVerdict(verdict: Verdict)

  fun error(message: String, e: Throwable?)

  fun info(message: String, e: Throwable?)

  fun info(message: String)

}