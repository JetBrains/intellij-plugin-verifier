package com.jetbrains.pluginverifier.options

import com.jetbrains.plugin.structure.intellij.problems.*
import com.jetbrains.pluginverifier.options.SubmissionType.EXISTING
import com.jetbrains.pluginverifier.options.SubmissionType.NEW
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val LOG: Logger = LoggerFactory.getLogger(PluginParsingConfigurationResolution::class.java)

const val EXISTING_PLUGIN_REMAPPING_SET = "existing-plugin"
const val NEW_PLUGIN_REMAPPING_SET = "new-plugin"

class PluginParsingConfigurationResolution {
  fun resolveProblemLevelMapping(configuration: PluginParsingConfiguration,
                                 problemLevelMappingManager: ProblemLevelRemappingManager): PluginCreationResultResolver {
    return when (configuration.pluginSubmissionType) {
      EXISTING -> getPluginCreationResultResolver(EXISTING_PLUGIN_REMAPPING_SET, problemLevelMappingManager)
      NEW -> getPluginCreationResultResolver(NEW_PLUGIN_REMAPPING_SET, problemLevelMappingManager)
    }
  }

}

private fun getPluginCreationResultResolver(levelRemappingDefinitionName: String, problemLevelMappingManager: ProblemLevelRemappingManager): LevelRemappingPluginCreationResultResolver {
  val defaultResolver = IntelliJPluginCreationResultResolver()
  val problemLevelRemapping = runCatching {
    val levelRemappings = problemLevelMappingManager.initialize()
    val levelRemappingDefinition = levelRemappings[levelRemappingDefinitionName]
      ?: emptyLevelRemapping(levelRemappingDefinitionName).also {
        LOG.warn(("Plugin problem remapping definition '$levelRemappingDefinitionName' was not found. " +
          "Problem levels will not be remapped"))
      }
    levelRemappingDefinition
  }.getOrElse {
    LOG.error(it.message, it)
    emptyMap()
  }
  return LevelRemappingPluginCreationResultResolver(defaultResolver, additionalLevelRemapping = problemLevelRemapping)
}
