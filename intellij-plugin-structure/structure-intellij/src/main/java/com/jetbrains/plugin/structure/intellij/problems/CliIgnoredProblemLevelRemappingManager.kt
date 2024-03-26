package com.jetbrains.plugin.structure.intellij.problems

import com.jetbrains.plugin.structure.base.problems.PluginProblem
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass

const val CLI_IGNORED = "cli-ignored"

typealias PluginProblemId = String

private val LOG: Logger = LoggerFactory.getLogger(CliIgnoredProblemLevelRemappingManager::class.java)

class CliIgnoredProblemLevelRemappingManager(ignoredProblems: List<PluginProblemId> = emptyList()) : ProblemLevelRemappingManager {
  private val problemClasses = mutableMapOf<PluginProblemId, KClass<out PluginProblem>>(
    "ForbiddenPluginIdPrefix" to ForbiddenPluginIdPrefix::class,
    "TemplateWordInPluginId" to TemplateWordInPluginId::class,
    "TemplateWordInPluginName" to TemplateWordInPluginName::class
  )

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
    return ignoredProblemClasses.associateWith { IgnoredLevel }
  }
}