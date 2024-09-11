package com.jetbrains.pluginverifier.options

import com.jetbrains.plugin.structure.intellij.problems.*
import com.jetbrains.plugin.structure.intellij.problems.remapping.ProblemLevelRemappingManager
import com.jetbrains.plugin.structure.intellij.problems.remapping.RemappingSet
import com.jetbrains.plugin.structure.intellij.problems.remapping.ignored.CliIgnoredProblemLevelRemappingManager
import com.jetbrains.pluginverifier.options.SubmissionType.EXISTING
import com.jetbrains.pluginverifier.options.SubmissionType.NEW

class PluginParsingConfigurationResolution {
  fun resolveProblemLevelMapping(configuration: PluginParsingConfiguration,
                                 problemLevelMappingManager: ProblemLevelRemappingManager
  ): PluginCreationResultResolver {
    val remappingSet = when (configuration.pluginSubmissionType) {
      EXISTING -> RemappingSet.EXISTING_PLUGIN_REMAPPING_SET
      NEW -> RemappingSet.NEW_PLUGIN_REMAPPING_SET
    }
    return problemLevelMappingManager
      .newDefaultResolver(remappingSet)
      .withCliIgnoredProblemResolver(configuration)
  }

  private fun PluginCreationResultResolver.withCliIgnoredProblemResolver(configuration: PluginParsingConfiguration): PluginCreationResultResolver {
    val cliIgnoredProblems = CliIgnoredProblemLevelRemappingManager(configuration.ignoredPluginProblems).asLevelRemappingDefinition()
    return LevelRemappingPluginCreationResultResolver(this, cliIgnoredProblems, unwrapRemappedProblems = true)
  }
}

