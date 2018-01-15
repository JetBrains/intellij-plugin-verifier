package com.jetbrains.pluginverifier.tasks.checkIde

import com.jetbrains.pluginverifier.dependencies.resolution.IdeDependencyFinder
import com.jetbrains.pluginverifier.misc.closeOnException
import com.jetbrains.pluginverifier.misc.isDirectory
import com.jetbrains.pluginverifier.misc.tryInvokeSeveralTimes
import com.jetbrains.pluginverifier.options.CmdOpts
import com.jetbrains.pluginverifier.options.OptionsParser
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.reporting.verification.VerificationReportage
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.tasks.TaskParametersBuilder
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

class CheckIdeParamsBuilder(val pluginRepository: PluginRepository,
                            val pluginDetailsCache: PluginDetailsCache,
                            val verificationReportage: VerificationReportage) : TaskParametersBuilder {
  override fun build(opts: CmdOpts, freeArgs: List<String>): CheckIdeParams {
    if (freeArgs.isEmpty()) {
      throw IllegalArgumentException("You have to specify IDE to check. For example: \"java -jar verifier.jar check-ide ~/EAPs/idea-IU-133.439\"")
    }
    val ideFile = Paths.get(freeArgs[0])
    if (!ideFile.isDirectory) {
      throw IllegalArgumentException("IDE path must be a directory: " + ideFile)
    }
    verificationReportage.logVerificationStage("Reading classes of IDE $ideFile")
    OptionsParser.createIdeDescriptor(ideFile, opts).closeOnException { ideDescriptor ->
      val jdkDescriptor = OptionsParser.createJdkDescriptor(opts)
      val externalClassesPrefixes = OptionsParser.getExternalClassesPrefixes(opts)
      OptionsParser.getExternalClassPath(opts).closeOnException { externalClassPath ->
        val problemsFilters = OptionsParser.getProblemsFilters(opts)

        val pluginsToCheck = tryInvokeSeveralTimes(3, 5, TimeUnit.SECONDS, "fetch updates to check with ${ideDescriptor.ideVersion}") {
          OptionsParser.parsePluginsToCheck(opts, ideDescriptor.ideVersion, pluginRepository)
        }

        val excludedPlugins = OptionsParser.parseExcludedPlugins(opts)
        val ideDependencyFinder = IdeDependencyFinder(ideDescriptor.ide, pluginRepository, pluginDetailsCache)
        return CheckIdeParams(
            pluginsToCheck,
            ideDescriptor,
            jdkDescriptor,
            excludedPlugins,
            externalClassesPrefixes,
            externalClassPath,
            externalClassesPrefixes,
            problemsFilters,
            ideDependencyFinder
        )
      }
    }
  }

}