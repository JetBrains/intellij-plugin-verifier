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
  class Nice(pluginDescriptor: PluginDescriptor, val overview: String) : VResult()
  class Problems(pluginDescriptor: PluginDescriptor, ideDescriptor: IdeDescriptor, val overview: String, val problems: Multimap<Problem, ProblemLocation>) : VResult()
  class BadPlugin(pluginDescriptor: PluginDescriptor, val reason: String) : VResult()
  class VerificationError(pluginDescriptor: PluginDescriptor, ideDescriptor: IdeDescriptor? = null, val message: String? = null, val exception: Exception? = null) : VResult()
}