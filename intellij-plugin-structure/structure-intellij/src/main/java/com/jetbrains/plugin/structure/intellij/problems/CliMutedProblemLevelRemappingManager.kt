package com.jetbrains.plugin.structure.intellij.problems

import com.jetbrains.plugin.structure.base.problems.PluginProblem
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass

const val CLI_MUTED = "cli-muted"

typealias PluginProblemId = String

private val LOG: Logger = LoggerFactory.getLogger(CliMutedProblemLevelRemappingManager::class.java)

class CliMutedProblemLevelRemappingManager(mutedProblems: List<PluginProblemId> = emptyList()) : ProblemLevelRemappingManager {
  private val problems = mutableMapOf<PluginProblemId, KClass<out PluginProblem>>(
    "ForbiddenPluginIdPrefix" to ForbiddenPluginIdPrefix::class,
    "TemplateWordInPluginId" to TemplateWordInPluginId::class,
    "TemplateWordInPluginName" to TemplateWordInPluginName::class
  )

  private val mutedProblemLevelRemapping: Map<KClass<*>, IgnoredLevel> = muteProblems(mutedProblems)

  override fun initialize(): LevelRemappingDefinitions {
    return LevelRemappingDefinitions().apply {
      set(CLI_MUTED, LevelRemappingDefinition(CLI_MUTED, mutedProblemLevelRemapping))
    }
  }

  fun asLevelRemappingDefinition(): LevelRemappingDefinition {
    return LevelRemappingDefinition(CLI_MUTED, mutedProblemLevelRemapping)
  }

  private fun muteProblems(mutedProblems: List<PluginProblemId>): Map<KClass<*>, IgnoredLevel> {
    val mutedProblemClasses = mutedProblems.mapNotNull { mutedProblemId ->
      val problemClass = problems[mutedProblemId]
      if (problemClass == null) {
        LOG.warn("Plugin problem '{}' cannot be muted. It is either not found or muting is not supported", mutedProblemId)
      }
      problemClass
    }
    return mutedProblemClasses.associateWith { IgnoredLevel }
  }
}