package com.jetbrains.pluginverifier.options

import com.jetbrains.plugin.structure.intellij.problems.JetBrainsPluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.problems.LevelRemappingPluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.problems.PluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.problems.remapping.JsonUrlProblemLevelRemappingManager
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
    val defaultResolver = problemLevelMappingManager.newDefaultResolver(remappingSet)
    return JetBrainsPluginCreationResultResolver.fromClassPathJson(delegatedResolver = defaultResolver)
      .withCliIgnoredProblemResolver(configuration)
  }

  private fun PluginCreationResultResolver.withCliIgnoredProblemResolver(configuration: PluginParsingConfiguration): PluginCreationResultResolver {
    val cliIgnoredProblems = CliIgnoredProblemLevelRemappingManager(configuration.ignoredPluginProblems).asLevelRemappingDefinition()
    return LevelRemappingPluginCreationResultResolver(this, cliIgnoredProblems, unwrapRemappedProblems = true)
  }

  companion object {
    fun of(opts: CmdOpts): PluginCreationResultResolver {
      val config = OptionsParser.createPluginParsingConfiguration(opts)
      return of(config)
    }

    fun of(configuration: PluginParsingConfiguration): PluginCreationResultResolver {
      val remappingManager = JsonUrlProblemLevelRemappingManager.fromClassPathJson()
      return PluginParsingConfigurationResolution().resolveProblemLevelMapping(configuration, remappingManager)
    }
  }
}
