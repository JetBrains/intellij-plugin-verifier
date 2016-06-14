package com.jetbrains.pluginverifier

import com.google.common.collect.Multimap
import com.jetbrains.pluginverifier.location.ProblemLocation
import com.jetbrains.pluginverifier.problems.Problem

/**
 * @author Sergey Patrikeev
 */
data class VResults(val results: List<PluginOnIdeResult>)

sealed class PluginOnIdeResult(val pluginOnIde: PluginOnIde) {
  class Success(pluginOnIde: PluginOnIde, val overview: String, val problems: Multimap<Problem, ProblemLocation>) : PluginOnIdeResult(pluginOnIde)
  class Error(pluginOnIde: PluginOnIde, val message: String? = null, val exception: Exception? = null) : PluginOnIdeResult(pluginOnIde)
}
