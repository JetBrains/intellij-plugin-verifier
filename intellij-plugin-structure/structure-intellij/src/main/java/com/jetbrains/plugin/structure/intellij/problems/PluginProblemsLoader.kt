package com.jetbrains.plugin.structure.intellij.problems

import com.fasterxml.jackson.core.exc.StreamReadException
import com.fasterxml.jackson.databind.DatabindException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.jetbrains.plugin.structure.intellij.problems.PluginProblemsLoader.PluginProblemLevel
import java.io.IOException
import java.net.URL

const val PLUGIN_PROBLEMS_FILE_NAME = "plugin-problems.json"

class PluginProblemsLoader(private val pluginProblemsJsonUrl: URL) {
  private val json = ObjectMapper()

  fun load(): PluginProblemLevelRemappingDefinitions {
    try {
      val rawRemapping: Map<String, Map<String, String>> = json.readValue(pluginProblemsJsonUrl)

      return rawRemapping.map { (problemSetName: String, problemRemapping: Map<String, String>) ->
        val levelMapping = problemRemapping.mapNotNull { (problemId, problemLevel) ->
          when (problemLevel) {
            "ignore" -> PluginProblemLevel.Ignored(problemId)
            "warning" -> PluginProblemLevel.Warning(problemId)
            "error" -> PluginProblemLevel.Error(problemId)
            else -> null
          }
        }.toSet()
        PluginProblemSet(problemSetName, levelMapping)
      }
        .toSet()
        .let {
          PluginProblemLevelRemappingDefinitions(it)
        }
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

  companion object {
    fun fromClassPath(): PluginProblemsLoader {
      val pluginProblemsJsonUrl = PluginProblemsLoader::class.java.getResource(PLUGIN_PROBLEMS_FILE_NAME)
        ?: throw IOException("Plugin problem level remapping definition cannot be found at <$this>")
      return PluginProblemsLoader(pluginProblemsJsonUrl)
    }
  }
}

data class PluginProblemSet(val name: String, val problems: Set<PluginProblemLevel>)

class PluginProblemLevelRemappingDefinitions(private val pluginProblemSetCollection: Set<PluginProblemSet>) {
  val size: Int
    get() = pluginProblemSetCollection.size

  operator fun get(setName: String): PluginProblemSet? {
    return pluginProblemSetCollection.find {
      it.name == setName
    }
  }
}