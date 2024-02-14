package com.jetbrains.pluginverifier.options

import com.jetbrains.plugin.structure.intellij.problems.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException

private val LOG: Logger = LoggerFactory.getLogger(PluginParsingConfigurationResolution::class.java)

private const val EXISTING_PLUGIN_REMAPPING_SET = "existing-plugin"

class PluginParsingConfigurationResolution {
  fun resolveProblemLevelMapping(configuration: PluginParsingConfiguration,
                                 problemLevelRemapperFactory: () -> PluginProblemLevelRemappingDefinitionManager): PluginCreationResultResolver {
    val defaultResolver = IntelliJPluginCreationResultResolver()
    return if (configuration.pluginSubmissionType == SubmissionType.EXISTING) {
      val problemLevelRemapping = try {
        val problemLevelRemapper = problemLevelRemapperFactory()
        val levelRemappings = problemLevelRemapper.initialize()
        val existingPluginLevelRemapping = levelRemappings[EXISTING_PLUGIN_REMAPPING_SET]
          ?: emptyLevelRemapping(EXISTING_PLUGIN_REMAPPING_SET).also {
            LOG.warn(("Plugin problem remapping definition '$EXISTING_PLUGIN_REMAPPING_SET' was not found. " +
              "Problem levels will not be remapped"))
          }
        existingPluginLevelRemapping
      } catch (e: IOException) {
        LOG.error(e.message, e)
        emptyMap()
      }

      LevelRemappingPluginCreationResultResolver(defaultResolver, additionalLevelRemapping = problemLevelRemapping)
    } else {
      defaultResolver
    }
  }
}
