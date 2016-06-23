package com.jetbrains.pluginverifier

import com.google.common.collect.Multimap
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
  class Nice(val pluginDescriptor: PluginDescriptor, val ideDescriptor: IdeDescriptor, val overview: String) : VResult()

  /**
   * The Plugin has compatibility problems with the IDE. They are listed in the [problems]
   */
  class Problems(val pluginDescriptor: PluginDescriptor, val ideDescriptor: IdeDescriptor, val overview: String, val problems: Multimap<Problem, ProblemLocation>) : VResult()

  /**
   * The Plugin has a completely incorrect structure (missing plugin.xml, etc.).
   * The [reason] is a user-friendly description of the problem.
   */
  class BadPlugin(val pluginDescriptor: PluginDescriptor, val reason: String, val exception: Exception? = null) : VResult()

}