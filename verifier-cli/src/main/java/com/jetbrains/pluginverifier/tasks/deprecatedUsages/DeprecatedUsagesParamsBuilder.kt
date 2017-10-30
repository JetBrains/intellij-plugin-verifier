package com.jetbrains.pluginverifier.tasks.deprecatedUsages

import com.jetbrains.pluginverifier.dependencies.resolution.IdeDependencyFinder
import com.jetbrains.pluginverifier.options.CmdOpts
import com.jetbrains.pluginverifier.options.OptionsParser
import com.jetbrains.pluginverifier.parameters.jdk.JdkDescriptor
import com.jetbrains.pluginverifier.plugin.PluginCoordinate
import com.jetbrains.pluginverifier.plugin.PluginDetailsProvider
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.tasks.TaskParametersBuilder
import java.io.File

class DeprecatedUsagesParamsBuilder(val pluginRepository: PluginRepository,
                                    val pluginDetailsProvider: PluginDetailsProvider) : TaskParametersBuilder {
  override fun build(opts: CmdOpts, freeArgs: List<String>): DeprecatedUsagesParams {
    if (freeArgs.isEmpty()) {
      throw IllegalArgumentException("You have to specify IDE which deprecated API usages are to be found. For example: \"java -jar verifier.jar check-ide ~/EAPs/idea-IU-133.439\"")
    }
    val ideFile = File(freeArgs[0])
    if (!ideFile.isDirectory) {
      throw IllegalArgumentException("IDE path must be a directory: " + ideFile)
    }
    val ideDescriptor = OptionsParser.createIdeDescriptor(ideFile, opts)
    val jdkDescriptor = OptionsParser.getJdkDir(opts)
    val (checkAllBuilds, checkLastBuilds) = OptionsParser.parsePluginsToCheck(opts)
    val updatesToCheck = OptionsParser.requestUpdatesToCheckByIds(checkAllBuilds, checkLastBuilds, ideDescriptor.ideVersion, pluginRepository)
    val pluginCoordinates = updatesToCheck.map { PluginCoordinate.ByUpdateInfo(it, pluginRepository) }
    val ideDependencyFinder = IdeDependencyFinder(ideDescriptor.ide, pluginRepository, pluginDetailsProvider)
    return DeprecatedUsagesParams(ideDescriptor, JdkDescriptor(jdkDescriptor), pluginCoordinates, ideDependencyFinder)
  }

}