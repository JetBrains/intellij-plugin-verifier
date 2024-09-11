package com.jetbrains.plugin.structure.intellij.problems.remapping.ignored

import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.base.problems.isInstance
import com.jetbrains.plugin.structure.intellij.problems.IgnoredLevel
import com.jetbrains.plugin.structure.intellij.problems.remapping.LevelRemappingDefinition
import com.jetbrains.plugin.structure.intellij.problems.remapping.LevelRemappingDefinitions
import com.jetbrains.plugin.structure.intellij.problems.remapping.ProblemLevelRemappingManager
import com.jetbrains.plugin.structure.intellij.problems.ProblemSolutionHintProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass

const val CLI_IGNORED = "cli-ignored"

typealias PluginProblemId = String

private val LOG: Logger = LoggerFactory.getLogger(CliIgnoredProblemLevelRemappingManager::class.java)

class CliIgnoredProblemLevelRemappingManager(ignoredProblems: List<PluginProblemId> = emptyList()) : ProblemLevelRemappingManager, ProblemSolutionHintProvider {
  private val ignoredProblemDefinitionLoader = CliIgnoredProblemDefinitionLoader.fromClassPathJson()

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

  private fun findIgnoredProblemDefinition(problem: PluginProblem): CliIgnoredPluginProblem? {
    val ignoredProblemDefinition = problemClasses.firstOrNull {
      if (it.pluginProblemClass != null) {
        problem.isInstance(it.pluginProblemClass!!)
      } else {
        false
      }
    }
    return ignoredProblemDefinition
  }

  override fun getProblemSolutionHint(problem: PluginProblem): String? {
    val problemDefinition = findIgnoredProblemDefinition(problem) ?: return null
    with(problemDefinition) {
      return "This plugin problem has been reported since $since. " +
        "If the plugin was previously uploaded to the JetBrains Marketplace, it can be suppressed using the `-mute $id` command-line switch."
    }
  }

}

