package com.jetbrains.pluginverifier.options

import com.jetbrains.plugin.structure.intellij.problems.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException

private val LOG: Logger = LoggerFactory.getLogger(PluginParsingConfigurationResolution::class.java)

class PluginParsingConfigurationResolution {
  fun resolveLevelRemapping(configuration: PluginParsingConfiguration,
                            pluginProblemsLoaderFactory: PluginProblemsLoaderFactory
                            = ClassPathPluginProblemsLoaderFactory): PluginCreationResultResolver {
    val defaultResolver = IntelliJPluginCreationResultResolver()
    return if (configuration.pluginSubmissionType == SubmissionType.EXISTING) {
      val problemLevelRemapping = try {
        val pluginProblemsLoader = pluginProblemsLoaderFactory.getPluginProblemsLoader()
        val problemLevelRemappingDefinitions = pluginProblemsLoader.load()
        val pluginProblemSet = problemLevelRemappingDefinitions["existing-plugin"]
          ?: emptyProblemLevelRemapping("existing-plugin")
        val parser = RemappedPluginProblemLevelParser()
        val remapping = parser.parse(pluginProblemSet)
        remapping
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
