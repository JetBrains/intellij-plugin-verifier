package com.jetbrains.plugin.structure.intellij.problems.remapping

import com.fasterxml.jackson.core.exc.StreamReadException
import com.fasterxml.jackson.databind.DatabindException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.jetbrains.plugin.structure.base.problems.PluginProblem.Level.*
import com.jetbrains.plugin.structure.base.problems.PluginProblems.resolveClass
import com.jetbrains.plugin.structure.intellij.problems.*
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.URL
import kotlin.reflect.KClass

private val LOG = LoggerFactory.getLogger(ProblemLevelRemappingManager::class.java)

interface ProblemLevelRemappingManager {
  @Throws(IOException::class)
  fun initialize(): LevelRemappingDefinitions

  fun getLevelRemapping(remappingSet: RemappingSet): LevelRemappingDefinition {
    val levelRemappings = runCatching {
      initialize()
    }.getOrElse {
      LOG.error(it.message, it)
      LevelRemappingDefinitions()
    }

    return levelRemappings[remappingSet]
      ?: LevelRemappingDefinition(remappingSet).also {
        LOG.warn(("Plugin problem remapping definition '${remappingSet.id}' was not found. " +
          "Problem levels will not be remapped"))
      }
  }

  fun newDefaultResolver(remappingSet: RemappingSet): PluginCreationResultResolver {
    val defaultResolver = IntelliJPluginCreationResultResolver()
    val problemLevelRemapping = getLevelRemapping(remappingSet)
    return LevelRemappingPluginCreationResultResolver(defaultResolver, problemLevelRemapping)
  }

  fun defaultExistingPluginResolver() = newDefaultResolver(RemappingSet.EXISTING_PLUGIN_REMAPPING_SET)
  fun defaultNewPluginResolver() = newDefaultResolver(RemappingSet.NEW_PLUGIN_REMAPPING_SET)
  fun defaultJetBrainsPluginResolver(defaultResolver: PluginCreationResultResolver) = LevelRemappingPluginCreationResultResolver(defaultResolver, getLevelRemapping(RemappingSet.JETBRAINS_PLUGIN_REMAPPING_SET), unwrapRemappedProblems = true)
}

class JsonUrlProblemLevelRemappingManager(private val pluginProblemsJsonUrl: URL) : ProblemLevelRemappingManager {
  companion object {
    private val objectMapper = ObjectMapper()
    const val PLUGIN_PROBLEMS_FILE_NAME = "plugin-problems.json"

    @Throws(IOException::class)
    fun fromClassPathJson(): JsonUrlProblemLevelRemappingManager {
      val pluginProblemsJsonUrl = this::class.java.getResource(PLUGIN_PROBLEMS_FILE_NAME)
        ?: throw IOException("Plugin problem level remapping definition cannot be found at <$PLUGIN_PROBLEMS_FILE_NAME>")
      return JsonUrlProblemLevelRemappingManager(pluginProblemsJsonUrl)
    }
  }

  @Throws(IOException::class)
  override fun initialize() = load()

  @Throws(IOException::class)
  fun load(): LevelRemappingDefinitions {
    val definitions = LevelRemappingDefinitions()
    try {
      val rawRemapping: Map<String, Map<String, String>> = objectMapper.readValue(pluginProblemsJsonUrl)

      rawRemapping.map { (id: String, problemRemapping: Map<String, String>) ->
        val remappingSet = RemappingSet.fromId(id)
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
        definitions[remappingSet] = levelRemapping
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
}

class LevelRemappingDefinitions {
  private val definitions = mutableMapOf<RemappingSet, LevelRemappingDefinition>()

  operator fun set(remappingSet: RemappingSet, remapping: Map<KClass<out Any>, RemappedLevel>) {
    definitions[remappingSet] = LevelRemappingDefinition(remappingSet, remapping)
  }

  operator fun get(remappingSet: RemappingSet): LevelRemappingDefinition? {
    return definitions[remappingSet]
  }

  fun getOrEmpty(remappingSet: RemappingSet): LevelRemappingDefinition {
    return definitions[remappingSet] ?: LevelRemappingDefinition(remappingSet)
  }

  val size: Int
    get() = definitions.size
}

class LevelRemappingDefinition(val remappingSet: RemappingSet, private val d: Map<KClass<out Any>, RemappedLevel> = emptyMap()) : Map<KClass<out Any>, RemappedLevel> by d {
  fun findProblemsByLevel(level: RemappedLevel): List<KClass<out Any>> {
    return d.filterValues { it == level }.map { it.key }
  }
}