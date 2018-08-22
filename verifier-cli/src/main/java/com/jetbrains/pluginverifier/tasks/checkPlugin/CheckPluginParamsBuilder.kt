package com.jetbrains.pluginverifier.tasks.checkPlugin

import com.jetbrains.pluginverifier.VerificationTarget
import com.jetbrains.pluginverifier.options.CmdOpts
import com.jetbrains.pluginverifier.options.OptionsParser
import com.jetbrains.pluginverifier.options.PluginsParsing
import com.jetbrains.pluginverifier.options.PluginsSet
import com.jetbrains.pluginverifier.reporting.verification.Reportage
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.tasks.TaskParametersBuilder
import java.nio.file.Paths

class CheckPluginParamsBuilder(
    val pluginRepository: PluginRepository,
    val reportage: Reportage
) : TaskParametersBuilder {

  override fun build(opts: CmdOpts, freeArgs: List<String>): CheckPluginParams {
    if (freeArgs.size <= 1) {
      throw IllegalArgumentException("You must specify plugin to check and IDE(s), example:\n" +
          "java -jar verifier.jar check-plugin ~/work/myPlugin/myPlugin.zip ~/EAPs/idea-IU-117.963\n" +
          "java -jar verifier.jar check-plugin #14986 ~/EAPs/idea-IU-117.963")
    }
    val ideDescriptors = freeArgs.drop(1).map { Paths.get(it) }.map {
      reportage.logVerificationStage("Reading IDE $it")
      OptionsParser.createIdeDescriptor(it, opts)
    }

    val ideVersions = ideDescriptors.map { it.ideVersion }
    val pluginsSet = PluginsSet()
    val pluginsParsing = PluginsParsing(pluginRepository, reportage, pluginsSet)

    val pluginToTestArg = freeArgs[0]
    when {
      pluginToTestArg.startsWith("@") -> {
        pluginsParsing.addPluginsFromFile(
            Paths.get(pluginToTestArg.substringAfter("@")),
            ideVersions
        )
      }
      pluginToTestArg.matches("#\\d+".toRegex()) -> {
        val updateId = Integer.parseInt(pluginToTestArg.drop(1))
        pluginsParsing.addUpdate(updateId)
      }
      else -> {
        pluginsParsing.addPluginFile(Paths.get(pluginToTestArg), true)
      }
    }

    pluginsSet.ignoredPlugins.forEach { plugin, reason ->
      ideVersions.forEach { ideVersion ->
        reportage.logPluginVerificationIgnored(plugin, VerificationTarget.Ide(ideVersion), reason)
      }
    }

    val externalClassesPackageFilter = OptionsParser.getExternalClassesPackageFilter(opts)
    val problemsFilters = OptionsParser.getProblemsFilters(opts)
    return CheckPluginParams(
        pluginsSet,
        OptionsParser.getJdkPath(opts),
        ideDescriptors,
        externalClassesPackageFilter,
        problemsFilters
    )
  }

}