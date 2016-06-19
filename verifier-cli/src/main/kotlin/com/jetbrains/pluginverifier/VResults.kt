package com.jetbrains.pluginverifier

import com.google.common.collect.Multimap
import com.jetbrains.pluginverifier.location.ProblemLocation
import com.jetbrains.pluginverifier.problems.Problem

/**
 * @author Sergey Patrikeev
 */
data class VResults(val results: List<PluginOnIdeResult>)

sealed class PluginOnIdeResult(val pluginDescriptor: PluginDescriptor, val ideDescriptor: IdeDescriptor) {
  class Success(pluginDescriptor: PluginDescriptor, ideDescriptor: IdeDescriptor, val overview: String, val problems: Multimap<Problem, ProblemLocation>) : PluginOnIdeResult(pluginDescriptor, ideDescriptor)
  class Error(pluginDescriptor: PluginDescriptor, ideDescriptor: IdeDescriptor, val message: String? = null, val exception: Exception? = null) : PluginOnIdeResult(pluginDescriptor, ideDescriptor)
}
