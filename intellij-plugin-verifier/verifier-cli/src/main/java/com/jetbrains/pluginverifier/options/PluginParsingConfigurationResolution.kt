package com.jetbrains.pluginverifier.options

import com.jetbrains.plugin.structure.intellij.problems.*
import com.jetbrains.pluginverifier.options.SubmissionType.EXISTING
import com.jetbrains.pluginverifier.options.SubmissionType.NEW

const val EXISTING_PLUGIN_REMAPPING_SET = "existing-plugin"
const val NEW_PLUGIN_REMAPPING_SET = "new-plugin"

class PluginParsingConfigurationResolution {
  fun resolveProblemLevelMapping(configuration: PluginParsingConfiguration,
                                 problemLevelMappingManager: ProblemLevelRemappingManager): PluginCreationResultResolver {
    return when (configuration.pluginSubmissionType) {
      EXISTING -> getPluginCreationResultResolver(EXISTING_PLUGIN_REMAPPING_SET, problemLevelMappingManager)
        .withJetBrainsPluginProblemLevelRemapping()

      NEW -> getPluginCreationResultResolver(NEW_PLUGIN_REMAPPING_SET, problemLevelMappingManager)
        .withJetBrainsPluginProblemLevelRemapping()
    }
  }
}

private fun getPluginCreationResultResolver(levelRemappingDefinitionName: String, problemLevelMappingManager: ProblemLevelRemappingManager): PluginCreationResultResolver {
  val defaultResolver = IntelliJPluginCreationResultResolver()
  val problemLevelRemapping = problemLevelMappingManager.getLevelRemapping(levelRemappingDefinitionName)
  val levelRemappingResolver = LevelRemappingPluginCreationResultResolver(defaultResolver, additionalLevelRemapping = problemLevelRemapping)
  return JetBrainsPluginCreationResultResolver.fromClassPathJson(delegatedResolver = levelRemappingResolver)
}

private fun PluginCreationResultResolver.withJetBrainsPluginProblemLevelRemapping() =
  JetBrainsPluginCreationResultResolver.fromClassPathJson(this)