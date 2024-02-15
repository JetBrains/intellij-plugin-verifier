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
                                 problemLevelRemapperFactory: () -> ProblemLevelRemappingManager): PluginCreationResultResolver {
    return when (configuration.pluginSubmissionType) {
      EXISTING -> getPluginCreationResultResolver(EXISTING_PLUGIN_REMAPPING_SET, problemLevelRemapperFactory)
      NEW -> getPluginCreationResultResolver(NEW_PLUGIN_REMAPPING_SET, problemLevelRemapperFactory)
    }
  }

  private fun getPluginCreationResultResolver(levelRemappingDefinitionName: String, problemLevelRemapperFactory: () -> ProblemLevelRemappingManager): LevelRemappingPluginCreationResultResolver {
    val defaultResolver = IntelliJPluginCreationResultResolver()
    val problemLevelRemapping = runCatching {
      val problemLevelRemapper = problemLevelRemapperFactory()
      val levelRemappings = problemLevelRemapper.initialize()
      val existingPluginLevelRemapping = levelRemappings[levelRemappingDefinitionName]
        ?: emptyLevelRemapping(levelRemappingDefinitionName).also {
          LOG.warn(("Plugin problem remapping definition '$levelRemappingDefinitionName' was not found. " +
            "Problem levels will not be remapped"))
        }
      existingPluginLevelRemapping
    }.getOrElse {
      LOG.error(it.message, it)
      emptyMap()
    }
    return LevelRemappingPluginCreationResultResolver(defaultResolver, additionalLevelRemapping = problemLevelRemapping)
  }
}
