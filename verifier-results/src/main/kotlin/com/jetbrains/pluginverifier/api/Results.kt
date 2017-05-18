package com.jetbrains.pluginverifier.api

import com.google.gson.annotations.SerializedName
import com.intellij.structure.ide.IdeVersion
import com.intellij.structure.problems.PluginProblem
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.dependencies.MissingDependency
import com.jetbrains.pluginverifier.format.UpdateInfo
import com.jetbrains.pluginverifier.misc.pluralize
import com.jetbrains.pluginverifier.problems.Problem
import com.jetbrains.pluginverifier.warnings.Warning

data class PluginInfo(@SerializedName("pluginId") val pluginId: String,
                      @SerializedName("version") val version: String,
                      @SerializedName("updateInfo") val updateInfo: UpdateInfo?) {
  override fun toString(): String = updateInfo?.toString() ?: "$pluginId:$version"
}

data class Result(@SerializedName("plugin") val plugin: PluginInfo,
                  @SerializedName("ideVersion") val ideVersion: IdeVersion,
                  @SerializedName("verdict") val verdict: Verdict)

sealed class Verdict {
  /**
   * Indicates that the Plugin doesn't have compatibility problems with the checked IDE.
   */
  data class OK(@SerializedName("depsGraph") val dependenciesGraph: DependenciesGraph) : Verdict() {
    override fun toString() = "OK"
  }

  /**
   * The plugin has minor problems listed in [warnings].
   */
  data class Warnings(@SerializedName("warnings") val warnings: Set<Warning>,
                      @SerializedName("depsGraph") val dependenciesGraph: DependenciesGraph) : Verdict() {
    override fun toString(): String = "Found ${warnings.size} " + "warning".pluralize(warnings.size)
  }

  /**
   * The plugin has some dependencies which were not found during the verification.
   * Look at the [dependenciesGraph] for details.
   *
   * Note: some of the problems might be caused by the missing dependencies (unresolved classes etc.).
   * Also the [problems] might be empty if the missed dependencies don't affect the compatibility with the IDE.
   */
  data class MissingDependencies(@SerializedName("missingDeps") val missingDependencies: List<MissingDependency>,
                                 @SerializedName("depsGraph") val dependenciesGraph: DependenciesGraph,
                                 @SerializedName("problems") val problems: Set<Problem>,
                                 @SerializedName("warnings") val warnings: Set<Warning>) : Verdict() {
    override fun toString(): String = "Missing plugins and modules dependencies: " +
        "${missingDependencies.joinToString()}; " +
        "and ${problems.size} " + "problem".pluralize(problems.size) + ": ${problems.take(10).map { it.getShortDescription() }}..."
  }

  /**
   * The Plugin has compatibility problems with the IDE. They are listed in the [problems].
   */
  data class Problems(@SerializedName("problems") val problems: Set<Problem>,
                      @SerializedName("depsGraph") val dependenciesGraph: DependenciesGraph,
                      @SerializedName("warnings") val warnings: Set<Warning>) : Verdict() {
    override fun toString(): String = "Found ${problems.size} compatibility " + "problem".pluralize(problems.size)
  }

  /**
   * The Plugin has an incorrect structure.
   */
  data class Bad(@SerializedName("pluginProblems") val pluginProblems: List<PluginProblem>) : Verdict() {
    override fun toString(): String = "Plugin is invalid: $pluginProblems"
  }

  /**
   * The plugin is not found during the verification.
   * Look at [reason] for details
   */
  data class NotFound(@SerializedName("reason") val reason: String) : Verdict() {
    override fun toString(): String = "Plugin is not found: $reason"
  }


}


