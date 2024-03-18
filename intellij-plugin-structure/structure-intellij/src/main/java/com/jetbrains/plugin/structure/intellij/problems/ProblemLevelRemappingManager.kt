package com.jetbrains.plugin.structure.intellij.problems

import com.fasterxml.jackson.core.exc.StreamReadException
import com.fasterxml.jackson.databind.DatabindException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.jetbrains.plugin.structure.base.problems.PluginProblem.Level.*
import org.slf4j.LoggerFactory
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

private const val PLUGIN_PROBLEM_PACKAGE_DEFAULT_PREFIX = "com.jetbrains.plugin.structure."

const val PLUGIN_PROBLEMS_FILE_NAME = "plugin-problems.json"

private val LOG = LoggerFactory.getLogger(JsonUrlProblemLevelRemappingManager::class.java)

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
            "unacceptable-warning" -> pluginProblemKClass to StandardLevel(UNACCEPTABLE_WARNING)
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
   * Resolves the problem ID to a fully qualified Kotlin class.
   *
   * The following formats are supported:
   *
   * - Fully qualified problem ID which corresponds to a class name, such as
   *    `com.jetbrains.plugin.structure.intellij.problems.ForbiddenPluginIdPrefix`
   * - Problem ID which can be resolved to a fully qualified class name in the `com.jetbrains.plugin.structure`
   *    package prefix, such as `intellij.problems.ForbiddenPluginIdPrefix`.
   */
  private fun resolveClass(problemId: String): KClass<out Any>? {
    val fqProblemId = if (problemId.startsWith(PLUGIN_PROBLEM_PACKAGE_DEFAULT_PREFIX)) {
      problemId
    } else {
      PLUGIN_PROBLEM_PACKAGE_DEFAULT_PREFIX + problemId
    }
    return runCatching {
      val pluginProblemJavaClass = Class.forName(fqProblemId, false, this.javaClass.getClassLoader())
      val kotlin = pluginProblemJavaClass.kotlin
      kotlin
    }.onFailure { t ->
      LOG.warn("Problem ID '$problemId' could not be resolved to a fully qualified class corresponding to a plugin problem: {}", t.message)
    }.getOrNull()
  }
}

fun ProblemLevelRemappingManager.getLevelRemapping(levelRemappingDefinitionName: String): LevelRemappingDefinition {
  return runCatching {
    val levelRemappings = initialize()
    val levelRemappingDefinition = levelRemappings[levelRemappingDefinitionName]
      ?: emptyLevelRemapping(levelRemappingDefinitionName).also {
        LOG.warn(("Plugin problem remapping definition '$levelRemappingDefinitionName' was not found. " +
          "Problem levels will not be remapped"))
      }
    levelRemappingDefinition
  }.getOrElse {
    LOG.error(it.message, it)
    emptyLevelRemapping(levelRemappingDefinitionName)
  }
}

fun ProblemLevelRemappingManager.newDefaultResolver(levelRemappingDefinitionName: String): PluginCreationResultResolver {
  val defaultResolver = IntelliJPluginCreationResultResolver()
  val problemLevelRemapping = getLevelRemapping(levelRemappingDefinitionName)
  val levelRemappingResolver = LevelRemappingPluginCreationResultResolver(defaultResolver, additionalLevelRemapping = problemLevelRemapping)
  return JetBrainsPluginCreationResultResolver.fromClassPathJson(delegatedResolver = levelRemappingResolver)
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