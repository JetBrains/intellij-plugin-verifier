package com.jetbrains.pluginverifier.options

import com.jetbrains.plugin.structure.intellij.problems.LevelRemappingPluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.problems.PluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.problems.ProblemLevelRemappingManager
import com.jetbrains.plugin.structure.intellij.problems.ignored.CliIgnoredProblemLevelRemappingManager
import com.jetbrains.plugin.structure.intellij.problems.newDefaultResolver
import com.jetbrains.pluginverifier.options.SubmissionType.EXISTING
import com.jetbrains.pluginverifier.options.SubmissionType.NEW

const val EXISTING_PLUGIN_REMAPPING_SET = "existing-plugin"
const val NEW_PLUGIN_REMAPPING_SET = "new-plugin"

class PluginParsingConfigurationResolution {
  fun resolveProblemLevelMapping(configuration: PluginParsingConfiguration,
                                 problemLevelMappingManager: ProblemLevelRemappingManager): PluginCreationResultResolver {
    val levelRemappingDefinitionName = when (configuration.pluginSubmissionType) {
      EXISTING -> EXISTING_PLUGIN_REMAPPING_SET
      NEW -> NEW_PLUGIN_REMAPPING_SET
    }
    return problemLevelMappingManager
      .newDefaultResolver(levelRemappingDefinitionName)
      .withCliIgnoredProblemResolver(configuration)
  }

  private fun PluginCreationResultResolver.withCliIgnoredProblemResolver(configuration: PluginParsingConfiguration): PluginCreationResultResolver {
    val cliIgnoredProblems = CliIgnoredProblemLevelRemappingManager(configuration.ignoredPluginProblems).asLevelRemappingDefinition()
    return LevelRemappingPluginCreationResultResolver(this, cliIgnoredProblems, unwrapRemappedProblems = true)
  }
}

