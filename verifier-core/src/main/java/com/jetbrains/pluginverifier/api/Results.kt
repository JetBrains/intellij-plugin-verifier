package com.jetbrains.pluginverifier.api

import com.intellij.structure.ide.IdeVersion
import com.intellij.structure.problems.PluginProblem
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.dependencies.MissingDependency
import com.jetbrains.pluginverifier.misc.pluralize
import com.jetbrains.pluginverifier.problems.Problem
import com.jetbrains.pluginverifier.repository.UpdateInfo
import com.jetbrains.pluginverifier.warnings.Warning

data class PluginInfo(val pluginId: String,
                      val version: String,
                      val updateInfo: UpdateInfo?) {
  override fun toString(): String = updateInfo?.toString() ?: "$pluginId:$version"
}

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
   * Look at the [dependenciesGraph] for details.
   *
   * Note: some of the problems might be caused by the missing dependencies (unresolved classes etc.).
   * Also the [problems] might be empty if the missed dependencies don't affect the compatibility with the IDE.
   */
  data class MissingDependencies(val missingDependencies: List<MissingDependency>,
                                 val dependenciesGraph: DependenciesGraph,
                                 val problems: Set<Problem>,
                                 val warnings: Set<Warning>) : Verdict() {
    override fun toString(): String = "Missing ${missingDependencies.size} plugins and modules " + "dependency".pluralize(missingDependencies.size) + " and ${problems.size} " + "problem".pluralize(problems.size)
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
    override fun toString(): String = "Plugin is invalid: $pluginProblems"
  }

  /**
   * The plugin is not found during the verification.
   * Look at [reason] for details
   */
  data class NotFound(val reason: String) : Verdict() {
    override fun toString(): String = "Plugin is not found: $reason"
  }


}


