package com.jetbrains.pluginverifier.tasks.checkPlugin

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.misc.closeOnException
import com.jetbrains.pluginverifier.misc.exists
import com.jetbrains.pluginverifier.misc.singletonOrEmpty
import com.jetbrains.pluginverifier.misc.tryInvokeSeveralTimes
import com.jetbrains.pluginverifier.options.CmdOpts
import com.jetbrains.pluginverifier.options.OptionsParser
import com.jetbrains.pluginverifier.reporting.verification.VerificationReportage
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.UpdateInfo
import com.jetbrains.pluginverifier.repository.local.LocalPluginRepository
import com.jetbrains.pluginverifier.tasks.TaskParametersBuilder
import java.io.File
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

class CheckPluginParamsBuilder(val pluginRepository: PluginRepository) : TaskParametersBuilder {

  override fun build(opts: CmdOpts, freeArgs: List<String>, verificationReportage: VerificationReportage): CheckPluginParams {
    if (freeArgs.size <= 1) {
      throw IllegalArgumentException("You must specify plugin to check and IDE(s), example:\n" +
          "java -jar verifier.jar check-plugin ~/work/myPlugin/myPlugin.zip ~/EAPs/idea-IU-117.963\n" +
          "java -jar verifier.jar check-plugin #14986 ~/EAPs/idea-IU-117.963")
    }
    val ideDescriptors = freeArgs.drop(1).map { Paths.get(it) }.map { OptionsParser.createIdeDescriptor(it, opts) }
    val pluginsToCheck = getPluginsToCheck(freeArgs[0], ideDescriptors.map { it.ideVersion })
    val jdkDescriptor = OptionsParser.createJdkDescriptor(opts)
    val externalClassesPrefixes = OptionsParser.getExternalClassesPrefixes(opts)
    val externalClasspath = OptionsParser.getExternalClassPath(opts)
    externalClasspath.closeOnException {
      val problemsFilters = OptionsParser.getProblemsFilters(opts)
      return CheckPluginParams(pluginsToCheck, ideDescriptors, jdkDescriptor, externalClassesPrefixes, problemsFilters, externalClasspath)
    }
  }

  private fun getPluginsToCheck(pluginToTestArg: String, ideVersions: List<IdeVersion>) =
      when {
        pluginToTestArg.startsWith("@") ->
          getPluginsToCheckFromFile(
              File(pluginToTestArg.substring(1)),
              ideVersions
          )
        pluginToTestArg.matches("#\\d+".toRegex()) -> {
          val updateId = Integer.parseInt(pluginToTestArg.drop(1))
          val updateInfo = pluginRepository.tryInvokeSeveralTimes(3, 5, TimeUnit.SECONDS, "get update information for update #$updateId") {
            getPluginInfoById(updateId)
          } ?: throw IllegalArgumentException("Update #$updateId is not found in the Plugin Repository")
          listOf(updateInfo)
        }
        else -> {
          val file = Paths.get(pluginToTestArg)
          if (!file.exists()) {
            throw IllegalArgumentException("The file $file doesn't exist")
          }
          LocalPluginRepository(file.toUri().toURL()).addLocalPlugin(file).singletonOrEmpty()
        }
      }

  private fun getPluginsToCheckFromFile(pluginListFile: File, ideVersions: List<IdeVersion>): List<PluginInfo> {
    val pluginPaths = pluginListFile.readLines().map { it.trim() }.filterNot { it.isEmpty() }
    val localPluginRepository = LocalPluginRepository(pluginListFile.toURI().toURL())
    return ideVersions.flatMap { ideVersion ->
      pluginPaths.flatMap { path ->
        if (path.startsWith("id:")) {
          getCompatiblePluginVersions(path.substringAfter("id:"), ideVersion)
        } else {
          var pluginFile = Paths.get(path)
          if (!pluginFile.isAbsolute) {
            pluginFile = pluginListFile.toPath().resolve(path)
          }
          if (!pluginFile.exists()) {
            throw RuntimeException("Plugin file '" + path + "' specified in '" + pluginListFile.absolutePath + "' doesn't exist")
          }
          localPluginRepository.addLocalPlugin(pluginFile).singletonOrEmpty()
        }
      }
    }
  }


  private fun getCompatiblePluginVersions(pluginId: String, ideVersion: IdeVersion): List<PluginInfo> {
    val allCompatibleUpdatesOfPlugin = pluginRepository.tryInvokeSeveralTimes(3, 5, TimeUnit.SECONDS, "fetch all compatible updates of plugin $pluginId with $ideVersion") {
      getAllCompatibleVersionsOfPlugin(ideVersion, pluginId)
    }
    return allCompatibleUpdatesOfPlugin.map { it as UpdateInfo }
  }


}