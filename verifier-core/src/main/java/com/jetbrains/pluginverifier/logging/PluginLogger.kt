package com.jetbrains.pluginverifier.logging

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.plugin.PluginCoordinate
import com.jetbrains.pluginverifier.results.Verdict

/**
 * @author Sergey Patrikeev
 */
interface PluginLogger {
  val plugin: PluginCoordinate

  val ideVersion: IdeVersion

  fun started()

  fun finished()

  fun logDependencyGraph(dependenciesGraph: DependenciesGraph)

  fun logVerdict(verdict: Verdict)

  fun error(message: String, e: Throwable?)

  fun info(message: String, e: Throwable?)

  fun info(message: String)

}