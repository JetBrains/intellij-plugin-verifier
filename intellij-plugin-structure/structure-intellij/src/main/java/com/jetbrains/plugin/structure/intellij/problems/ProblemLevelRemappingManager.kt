package com.jetbrains.plugin.structure.intellij.problems

import com.fasterxml.jackson.core.exc.StreamReadException
import com.fasterxml.jackson.databind.DatabindException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.jetbrains.plugin.structure.base.problems.PluginProblem.Level.ERROR
import com.jetbrains.plugin.structure.base.problems.PluginProblem.Level.WARNING
import java.io.IOException
import java.net.URL
import kotlin.reflect.KClass

interface ProblemLevelRemappingManager {
  fun initialize(): LevelRemappingDefinitions
}

fun levelRemappingFromClassPathJson(): JsonUrlProblemLevelRemappingManager {
  val pluginProblemsJsonUrl = JsonUrlProblemLevelRemappingManager::class.java.getResource(PLUGIN_PROBLEMS_FILE_NAME)
    ?: throw IOException("Plugin problem level remapping definition cannot be found at <$PLUGIN_PROBLEMS_FILE_NAME>")
  return JsonUrlProblemLevelRemappingManager(pluginProblemsJsonUrl)
}

private const val INTELLIJ_PROBLEMS_PACKAGE_NAME = "com.jetbrains.plugin.structure.intellij.problems"
const val PLUGIN_PROBLEMS_FILE_NAME = "plugin-problems.json"

class JsonUrlProblemLevelRemappingManager(private val pluginProblemsJsonUrl: URL) : ProblemLevelRemappingManager {
  private val json = ObjectMapper()

  @Throws(IOException::class)
  override fun initialize() = load()

  @Throws(IOException::class)
  fun load(): LevelRemappingDefinitions {
    val definitions = LevelRemappingDefinitions()
    try {
      val rawRemapping: Map<String, Map<String, String>> = json.readValue(pluginProblemsJsonUrl)

      rawRemapping.map { (problemSetName: String, problemRemapping: Map<String, String>) ->
        val levelRemapping = problemRemapping.mapNotNull { (problemId, problemLevel) ->
          val pluginProblemKClass = resolveClass(problemId) ?: return@mapNotNull null
          when (problemLevel) {
            "ignore" -> pluginProblemKClass to IgnoredLevel
            "warning" -> pluginProblemKClass to StandardLevel(WARNING)
            "error" -> pluginProblemKClass to StandardLevel(ERROR)
            else -> null
          }
        }.toMap()
        definitions[problemSetName] = levelRemapping
      }
    } catch (e: IOException) {
      throw IOException("Cannot load plugin problems definitions from <$pluginProblemsJsonUrl>", e)
    } catch (e: StreamReadException) {
      throw IOException("Cannot parse plugin problems definitions from JSON in <$pluginProblemsJsonUrl>", e)
    } catch (e: DatabindException) {
      throw IOException("Cannot deserialize plugin problems definitions from JSON in <$pluginProblemsJsonUrl>", e)
    }
    return definitions
  }


  /**
   * Resolves the problem ID to a fully qualified class name.
   *
   * Example: `ForbiddenPluginIdPrefix` to `com.jetbrains.plugin.structure.intellij.problems.ForbiddenPluginIdPrefix`
   */
  private fun resolveClass(problemId: String): KClass<out Any>? {
    val fqn = "$INTELLIJ_PROBLEMS_PACKAGE_NAME.$problemId"
    return runCatching {
      val pluginProblemJavaClass = Class.forName(fqn, false, this.javaClass.getClassLoader())
      val kotlin = pluginProblemJavaClass.kotlin
      kotlin
    }.getOrNull()
  }
}

class LevelRemappingDefinitions {
  private val definitions = mutableMapOf<String, LevelRemappingDefinition>()

  operator fun set(name: String, remapping: Map<KClass<out Any>, RemappedLevel>) {
    definitions[name] = LevelRemappingDefinition(name, remapping)
  }

  operator fun get(name: String): LevelRemappingDefinition? {
    return definitions[name]
  }

  val size: Int
    get() = definitions.size
}

class LevelRemappingDefinition(val name: String, private val d: Map<KClass<out Any>, RemappedLevel> = emptyMap()) : Map<KClass<out Any>, RemappedLevel> by d {
  fun findProblemsByLevel(level: RemappedLevel): List<KClass<out Any>> {
    return d.filterValues { it == level }.map { it.key }
  }
}

fun emptyLevelRemapping(name: String): LevelRemappingDefinition {
  return LevelRemappingDefinition(name, emptyMap())
}