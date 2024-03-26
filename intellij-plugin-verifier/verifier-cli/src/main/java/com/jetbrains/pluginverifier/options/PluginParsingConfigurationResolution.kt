package com.jetbrains.pluginverifier.options

import com.jetbrains.plugin.structure.intellij.problems.*
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
      .withCliMutedProblemResolver(configuration)
  }

  private fun PluginCreationResultResolver.withCliMutedProblemResolver(configuration: PluginParsingConfiguration): PluginCreationResultResolver {
    val cliMutedProblems = CliMutedProblemLevelRemappingManager(configuration.ignoredPluginProblems).asLevelRemappingDefinition()
    return LevelRemappingPluginCreationResultResolver(this, cliMutedProblems, unwrapRemappedProblems = true)
  }
}

