package com.jetbrains.plugin.structure.intellij.problems

import com.fasterxml.jackson.core.exc.StreamReadException
import com.fasterxml.jackson.databind.DatabindException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.jetbrains.plugin.structure.intellij.problems.PluginProblemsLoader.PluginProblemLevel
import java.io.IOException
import java.net.URL

class PluginProblemsLoader(private val pluginProblemsJsonUrl: URL) {
  private val json = ObjectMapper()

  private var _pluginProblemSetCollection = mutableSetOf<PluginProblemSet>()

  val pluginProblemSetCollection: Set<PluginProblemSet>
    get() = _pluginProblemSetCollection

  fun load() {
    try {
      val result: Map<String, Map<String, String>> = json.readValue(pluginProblemsJsonUrl)

      _pluginProblemSetCollection = result.map { (problemSetName: String, problems: Map<String, String>) ->
        val levelMapping = problems.mapNotNull { (problemId, problemLevel) ->
          when (problemLevel) {
            "ignore" -> PluginProblemLevel.Ignored(problemId)
            "warning" -> PluginProblemLevel.Warning(problemId)
            "error" -> PluginProblemLevel.Error(problemId)
            else -> null
          }
        }.toSet()
        PluginProblemSet(problemSetName, levelMapping)
      }.toMutableSet()
    } catch (e: IOException) {
      throw IOException("Cannot load plugin problems definitions from <$pluginProblemsJsonUrl>", e)
    } catch (e: StreamReadException) {
      throw IOException("Cannot parse plugin problems definitions from JSON in <$pluginProblemsJsonUrl>", e)
    } catch (e: DatabindException) {
      throw IOException("Cannot deserialize plugin problems definitions from JSON in <$pluginProblemsJsonUrl>", e)
    }
  }

  sealed class PluginProblemLevel {
    abstract val problemId: String

    data class Ignored(override val problemId: String) : PluginProblemLevel()
    data class Warning(override val problemId: String) : PluginProblemLevel()
    data class Error(override val problemId: String) : PluginProblemLevel()
  }
}

data class PluginProblemSet(val name: String, val problems: Set<PluginProblemLevel>)

operator fun Set<PluginProblemSet>.get(setName: String): PluginProblemSet? {
  return this.find {
    it.name == setName
  }
}