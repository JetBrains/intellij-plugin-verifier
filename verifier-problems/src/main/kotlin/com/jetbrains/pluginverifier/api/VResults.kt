package com.jetbrains.pluginverifier.api

import com.google.common.collect.Multimap
import com.google.gson.annotations.SerializedName
import com.jetbrains.pluginverifier.location.ProblemLocation
import com.jetbrains.pluginverifier.problems.Problem

/**
 * @author Sergey Patrikeev
 */
data class VResults(val results: List<VResult>) {
  constructor(result: VResult) : this(listOf(result))
}

sealed class VResult() {
  /**
   * Indicates that the Plugin doesn't have compatibility problems with the checked IDE.
   */
  class Nice(@SerializedName("plugin") val pluginDescriptor: PluginDescriptor, @SerializedName("ide") val ideDescriptor: IdeDescriptor, @SerializedName("overview") val overview: String) : VResult() {
    override fun toString(): String {
      return "VResult.Nice(plugin=$pluginDescriptor, ide=$ideDescriptor, overview='$overview')"
    }
  }

  /**
   * The Plugin has compatibility problems with the IDE. They are listed in the [problems]
   */
  class Problems(@SerializedName("plugin") val pluginDescriptor: PluginDescriptor, @SerializedName("ide") val ideDescriptor: IdeDescriptor, @SerializedName("overview") val overview: String, @SerializedName("problems") val problems: Multimap<Problem, ProblemLocation>) : VResult() {

    override fun toString(): String {
      return "VResult.Problems(plugin=$pluginDescriptor, ide=$ideDescriptor, overview='$overview', problems=$problems)"
    }

  }

  /**
   * The Plugin has a completely incorrect structure (missing plugin.xml, etc.).
   * The [reason] is a user-friendly description of the problem.
   */
  class BadPlugin(@SerializedName("plugin") val pluginDescriptor: PluginDescriptor, @SerializedName("reason") val reason: String) : VResult() {

    override fun toString(): String {
      return "VResult.BadPlugin(plugin=$pluginDescriptor, reason='$reason')"
    }


  }

}


