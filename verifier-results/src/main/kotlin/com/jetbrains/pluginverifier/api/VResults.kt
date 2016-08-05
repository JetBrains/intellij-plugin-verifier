package com.jetbrains.pluginverifier.api

import com.google.common.collect.Multimap
import com.google.gson.annotations.SerializedName
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.location.ProblemLocation
import com.jetbrains.pluginverifier.problems.Problem
import com.jetbrains.pluginverifier.warnings.Warning

/**
 * @author Sergey Patrikeev
 */
data class VResults(@SerializedName("results") val results: List<VResult>)

sealed class VResult(@SerializedName("plugin") val pluginDescriptor: PluginDescriptor,
                     @SerializedName("ide") val ideDescriptor: IdeDescriptor) {
  /**
   * Indicates that the Plugin doesn't have compatibility problems with the checked IDE.
   */
  class Nice(pluginDescriptor: PluginDescriptor,
             ideDescriptor: IdeDescriptor,
             @SerializedName("warnings") val warnings: List<Warning>) : VResult(pluginDescriptor, ideDescriptor) {

    override fun toString(): String = "VResult.Nice(plugin=$pluginDescriptor, ide=$ideDescriptor, warnings='$warnings')"
  }

  /**
   * The Plugin has compatibility problems with the IDE. They are listed in the [problems]
   */
  class Problems(pluginDescriptor: PluginDescriptor,
                 ideDescriptor: IdeDescriptor,
                 @SerializedName("problems") val problems: Multimap<Problem, ProblemLocation>,
                 @SerializedName("depsGraph") val dependenciesGraph: DependenciesGraph,
                 @SerializedName("warnings") val warnings: List<Warning>) : VResult(pluginDescriptor, ideDescriptor) {

    override fun toString(): String = "VResult.Problems(plugin=$pluginDescriptor, ide=$ideDescriptor, warnings='$warnings', problems=$problems)"

  }

  /**
   * The Plugin has a completely incorrect structure (missing plugin.xml, broken class-files, etc...)
   * The [reason] is a user-friendly description of the problem.
   */
  class BadPlugin(pluginDescriptor: PluginDescriptor,
                  @SerializedName("reason") val reason: String) : VResult(pluginDescriptor, IdeDescriptor.AnyIde) {

    override fun toString(): String = "VResult.BadPlugin(plugin=$pluginDescriptor, reason='$reason')"

  }

  /**
   * The plugin specified with [pluginDescriptor] is not found in the Repository.
   */
  class NotFound(pluginDescriptor: PluginDescriptor,
                 ideDescriptor: IdeDescriptor,
                 @SerializedName("reason") val reason: String) : VResult(pluginDescriptor, ideDescriptor) {

    override fun toString(): String = "VResult.NotFound(plugin=$pluginDescriptor, ideDescriptor=$ideDescriptor, reason=$reason)"
  }


}


