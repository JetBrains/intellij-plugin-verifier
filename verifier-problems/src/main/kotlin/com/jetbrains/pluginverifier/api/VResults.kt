package com.jetbrains.pluginverifier.api

import com.google.common.collect.Multimap
import com.jetbrains.pluginverifier.location.ProblemLocation
import com.jetbrains.pluginverifier.persistence.Jsonable
import com.jetbrains.pluginverifier.persistence.fromGson
import com.jetbrains.pluginverifier.persistence.toGson
import com.jetbrains.pluginverifier.problems.Problem

/**
 * @author Sergey Patrikeev
 */
data class VResults(val results: List<VResult>) {
  constructor(result: VResult) : this(listOf(result))
}

sealed class VResult() : Jsonable<VResult> {
  /**
   * Indicates that the Plugin doesn't have compatibility problems with the checked IDE.
   */
  class Nice(val pluginDescriptor: PluginDescriptor, val ideDescriptor: IdeDescriptor, val overview: String) : VResult() {
    override fun serialize(): List<Pair<String, String>> = listOf(Pair("plugin", pluginDescriptor.toGson()), Pair("ide", ideDescriptor.toGson()), Pair("overview", overview))

    override fun deserialize(vararg params: String?): VResult = Nice(params[0]!!.fromGson(), params[1]!!.fromGson(), params[2]!!.fromGson())
  }

  /**
   * The Plugin has compatibility problems with the IDE. They are listed in the [problems]
   */
  class Problems(val pluginDescriptor: PluginDescriptor, val ideDescriptor: IdeDescriptor, val overview: String, val problems: Multimap<Problem, ProblemLocation>) : VResult() {
    override fun serialize(): List<Pair<String, String>> = listOf(Pair("plugin", pluginDescriptor.toGson()), Pair("ide", ideDescriptor.toGson()), Pair("overview", overview), Pair("problems", problems.toGson()))

    override fun deserialize(vararg params: String?): VResult = Problems(params[0]!!.fromGson(), params[1]!!.fromGson(), params[2]!!.fromGson(), params[3]!!.fromGson())

  }

  /**
   * The Plugin has a completely incorrect structure (missing plugin.xml, etc.).
   * The [reason] is a user-friendly description of the problem.
   */
  class BadPlugin(val pluginDescriptor: PluginDescriptor, val reason: String) : VResult() {
    override fun serialize(): List<Pair<String, String>> = listOf(Pair("plugin", pluginDescriptor.toGson()), Pair("reason", reason))

    override fun deserialize(vararg params: String?): VResult = BadPlugin(params[0]!!.fromGson(), params[1]!!.fromGson())
  }

}


