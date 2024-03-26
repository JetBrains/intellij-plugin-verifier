package com.jetbrains.plugin.structure.intellij.problems

import com.fasterxml.jackson.core.exc.StreamReadException
import com.fasterxml.jackson.databind.DatabindException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.base.problems.PluginProblems
import com.jetbrains.plugin.structure.base.problems.isInstance
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.URL
import kotlin.reflect.KClass

const val CLI_IGNORED = "cli-ignored"

typealias PluginProblemId = String

private val LOG: Logger = LoggerFactory.getLogger(CliIgnoredProblemLevelRemappingManager::class.java)

class CliIgnoredProblemLevelRemappingManager(ignoredProblems: List<PluginProblemId> = emptyList()) : ProblemLevelRemappingManager, ProblemSolutionHintProvider {
  private val ignoredProblemDefinitionLoader = CliIgnoredProblemDefinitionLoader()

  private val problemClasses: List<CliIgnoredPluginProblem> = ignoredProblemDefinitionLoader.load()

  private val ignoredProblemLevelRemapping: Map<KClass<*>, IgnoredLevel> = ignoreProblems(ignoredProblems)

  override fun initialize(): LevelRemappingDefinitions {
    return LevelRemappingDefinitions().apply {
      set(CLI_IGNORED, LevelRemappingDefinition(CLI_IGNORED, ignoredProblemLevelRemapping))
    }
  }

  fun asLevelRemappingDefinition(): LevelRemappingDefinition {
    return LevelRemappingDefinition(CLI_IGNORED, ignoredProblemLevelRemapping)
  }

  private fun ignoreProblems(ignoredProblems: List<PluginProblemId>): Map<KClass<*>, IgnoredLevel> {
    val ignoredProblemClasses = ignoredProblems.mapNotNull { mutedProblemId ->
      val problemClass = problemClasses[mutedProblemId]
      if (problemClass == null) {
        LOG.warn("Plugin problem '{}' cannot be muted or ignored. It is either not found or not supported", mutedProblemId)
      }
      problemClass
    }
    return ignoredProblemClasses
      .mapNotNull { it.pluginProblemClass }
      .associateWith { IgnoredLevel }
  }

  private operator fun List<CliIgnoredPluginProblem>.get(pluginProblemId: PluginProblemId): CliIgnoredPluginProblem? {
    return problemClasses.firstOrNull {
      it.id == pluginProblemId
    }
  }

  private fun findProblemId(problem: PluginProblem): String? {
    val ignoredProblemDefinition = problemClasses.firstOrNull {
      if (it.pluginProblemClass != null) {
        problem.isInstance(it.pluginProblemClass!!)
      } else {
        false
      }
    }
    return ignoredProblemDefinition?.id
  }

  override fun getProblemSolutionHint(problem: PluginProblem): String? {
    val problemId = findProblemId(problem)

    //FIXME novotnyr handle dates
    val message = "This plugin problem has been reported since <___>. " +
      "If the plugin was previously uploaded to the JetBrains Marketplace, it can be suppressed using the `-mute $problemId` command-line switch."
    return message
  }

}

data class CliIgnoredPluginProblem(val id: String, val pluginProblemClassFqn: String, val since: String) {
  val pluginProblemClass: KClass<*>?
    get() {
      return PluginProblems.resolveClass(pluginProblemClassFqn)
    }
}

private const val CLI_IGNORED_PROBLEMS_FILE_NAME = "plugin-problems-cli-muteable.json"

class CliIgnoredProblemDefinitionLoader(private val jsonUrl: URL) {
  constructor() : this(
    CliIgnoredProblemLevelRemappingManager::class.java.getResource(CLI_IGNORED_PROBLEMS_FILE_NAME)
      ?: throw IOException("Definition for problems that can be ignored in the CLI switch " +
        "cannot be found at <$CLI_IGNORED_PROBLEMS_FILE_NAME>")
  )

  private val json = ObjectMapper()

  @Throws(IOException::class)
  fun load(): List<CliIgnoredPluginProblem> {
    return try {
      val rawDefinitions: List<Map<String, String>> = json.readValue(jsonUrl)
      rawDefinitions.map {
        CliIgnoredPluginProblem(
          it["id"] ?: throw IOException("Missing 'id' field in the definition of a CLI ignored problem"),
          it["class"]
            ?: throw IOException("Missing 'class' field in the definition of a CLI ignored problem"),
          it["since"] ?: throw IOException("Missing 'since' field in the definition of a CLI ignored problem")
        )
      }
    } catch (e: IOException) {
      throw IOException("Cannot load CLI ignorable problems definitions from <$jsonUrl>", e)
    } catch (e: StreamReadException) {
      throw IOException("Cannot parse CLI ignorable problems definitions from JSON in <$jsonUrl>", e)
    } catch (e: DatabindException) {
      throw IOException("Cannot deserialize CLI ignorable problems definitions from JSON in <$jsonUrl>", e)
    }
  }

}
