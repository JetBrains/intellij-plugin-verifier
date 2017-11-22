package com.jetbrains.pluginverifier.tasks.checkIde

import com.jetbrains.pluginverifier.dependencies.resolution.IdeDependencyFinder
import com.jetbrains.pluginverifier.misc.closeOnException
import com.jetbrains.pluginverifier.misc.isDirectory
import com.jetbrains.pluginverifier.misc.tryInvokeSeveralTimes
import com.jetbrains.pluginverifier.options.CmdOpts
import com.jetbrains.pluginverifier.options.OptionsParser
import com.jetbrains.pluginverifier.parameters.jdk.JdkDescriptor
import com.jetbrains.pluginverifier.plugin.PluginCoordinate
import com.jetbrains.pluginverifier.plugin.PluginDetailsProvider
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.tasks.TaskParametersBuilder
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

class CheckIdeParamsBuilder(val pluginRepository: PluginRepository, val pluginDetailsProvider: PluginDetailsProvider) : TaskParametersBuilder {
  override fun build(opts: CmdOpts, freeArgs: List<String>): CheckIdeParams {
    if (freeArgs.isEmpty()) {
      throw IllegalArgumentException("You have to specify IDE to check. For example: \"java -jar verifier.jar check-ide ~/EAPs/idea-IU-133.439\"")
    }
    val ideFile = Paths.get(freeArgs[0])
    if (!ideFile.isDirectory) {
      throw IllegalArgumentException("IDE path must be a directory: " + ideFile)
    }
    OptionsParser.createIdeDescriptor(ideFile, opts).closeOnException { ideDescriptor ->
      val jdkDescriptor = JdkDescriptor(OptionsParser.getJdkDir(opts))
      val externalClassesPrefixes = OptionsParser.getExternalClassesPrefixes(opts)
      OptionsParser.getExternalClassPath(opts).closeOnException { externalClassPath ->
        val problemsFilters = OptionsParser.getProblemsFilters(opts)

        val (checkAllBuilds, checkLastBuilds) = OptionsParser.parsePluginsToCheck(opts)

        val pluginsToCheck = this.tryInvokeSeveralTimes(3, 5, TimeUnit.SECONDS, "fetch updates to check with ${ideDescriptor.ideVersion}") {
          OptionsParser.requestUpdatesToCheckByIds(checkAllBuilds, checkLastBuilds, ideDescriptor.ideVersion, pluginRepository)
              .map { PluginCoordinate.ByUpdateInfo(it, pluginRepository) }
        }

        val excludedPlugins = OptionsParser.parseExcludedPlugins(opts)
        val ideDependencyFinder = IdeDependencyFinder(ideDescriptor.ide, pluginRepository, pluginDetailsProvider)
        return CheckIdeParams(ideDescriptor, jdkDescriptor, pluginsToCheck, excludedPlugins, externalClassesPrefixes, externalClassPath, checkAllBuilds, problemsFilters, ideDependencyFinder)
      }
    }
  }

}