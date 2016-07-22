package com.jetbrains.pluginverifier.api

import com.google.common.collect.Multimap
import com.google.gson.annotations.SerializedName
import com.jetbrains.pluginverifier.location.ProblemLocation
import com.jetbrains.pluginverifier.problems.Problem

/**
 * @author Sergey Patrikeev
 */
data class VResults(@SerializedName("results") val results: List<VResult>)

sealed class VResult(@SerializedName("plugin") val pluginDescriptor: PluginDescriptor,
                     @SerializedName("ide") val ideDescriptor: IdeDescriptor,
                     @SerializedName("overview") val overview: String) {
  /**
   * Indicates that the Plugin doesn't have compatibility problems with the checked IDE.
   */
  class Nice(pluginDescriptor: PluginDescriptor,
             ideDescriptor: IdeDescriptor,
             overview: String) : VResult(pluginDescriptor, ideDescriptor, overview) {

    override fun toString(): String = "VResult.Nice(plugin=$pluginDescriptor, ide=$ideDescriptor, overview='$overview')"
  }

  /**
   * The Plugin has compatibility problems with the IDE. They are listed in the [problems]
   */
  class Problems(pluginDescriptor: PluginDescriptor,
                 ideDescriptor: IdeDescriptor,
                 overview: String,
                 @SerializedName("problems") val problems: Multimap<Problem, ProblemLocation>) : VResult(pluginDescriptor, ideDescriptor, overview) {

    override fun toString(): String = "VResult.Problems(plugin=$pluginDescriptor, ide=$ideDescriptor, overview='$overview', problems=$problems)"

  }

  /**
   * The Plugin has a completely incorrect structure (missing plugin.xml, broken class-files, etc...)
   * The [reason] is a user-friendly description of the problem.
   */
  class BadPlugin(pluginDescriptor: PluginDescriptor,
                  reason: String) : VResult(pluginDescriptor, IdeDescriptor.AnyIde, reason) {

    override fun toString(): String = "VResult.BadPlugin(plugin=$pluginDescriptor, reason='$overview')"

  }

  /**
   * The plugin specified with [pluginDescriptor] is not found in the Repository.
   */
  class NotFound(pluginDescriptor: PluginDescriptor,
                 ideDescriptor: IdeDescriptor,
                 reason: String) : VResult(pluginDescriptor, ideDescriptor, reason) {

    override fun toString(): String = "VResult.NotFound(plugin=$pluginDescriptor, ideDescriptor=$ideDescriptor, reason=$overview)"
  }


}


