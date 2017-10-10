package com.jetbrains.pluginverifier.results

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.misc.pluralize
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.results.problems.Problem
import com.jetbrains.pluginverifier.results.warnings.Warning

data class Result(val plugin: PluginInfo,
                  val ideVersion: IdeVersion,
                  val verdict: Verdict) {
  override fun toString(): String = "Plugin $plugin and #$ideVersion: $verdict"
}

sealed class Verdict {
  /**
   * Indicates that the Plugin doesn't have compatibility problems with the checked IDE.
   */
  data class OK(val dependenciesGraph: DependenciesGraph) : Verdict() {
    override fun toString() = "OK"
  }

  /**
   * The plugin has minor problems listed in [warnings].
   */
  data class Warnings(val warnings: Set<Warning>,
                      val dependenciesGraph: DependenciesGraph) : Verdict() {
    override fun toString(): String = "Found ${warnings.size} " + "warning".pluralize(warnings.size)
  }

  /**
   * The plugin has some dependencies which were not found during the verification.
   *
   * Note: some of the problems might have been caused by the missing dependencies (unresolved classes etc.).
   * Also the [problems] might be empty if the missed dependencies don't affect the compatibility with the IDE.
   */
  data class MissingDependencies(val dependenciesGraph: DependenciesGraph,
                                 val problems: Set<Problem>,
                                 val warnings: Set<Warning>) : Verdict() {
    override fun toString(): String = "Missing ${directMissingDependencies.size} direct plugins and modules " + "dependency".pluralize(directMissingDependencies.size) + " and ${problems.size} " + "problem".pluralize(problems.size)

    val directMissingDependencies = dependenciesGraph.start.missingDependencies
    
  }

  /**
   * The Plugin has compatibility problems with the IDE. They are listed in the [problems].
   */
  data class Problems(val problems: Set<Problem>,
                      val dependenciesGraph: DependenciesGraph,
                      val warnings: Set<Warning>) : Verdict() {
    override fun toString(): String = "Found ${problems.size} compatibility " + "problem".pluralize(problems.size) + " and ${warnings.size} " + "warning".pluralize(warnings.size)
  }

  /**
   * The Plugin has an incorrect structure.
   */
  data class Bad(val pluginProblems: List<PluginProblem>) : Verdict() {
    override fun toString(): String = "Plugin is invalid: ${pluginProblems.joinToString()}"
  }

  /**
   * The plugin is not found during the verification.
   * Look at [reason] for details
   */
  data class NotFound(val reason: String) : Verdict() {
    override fun toString(): String = "Plugin is not found: $reason"
  }

  /**
   * The plugin is found in the Repository but the verifier failed to download it.
   * Look at [reason] for details
   */
  data class FailedToDownload(val reason: String) : Verdict()

}


