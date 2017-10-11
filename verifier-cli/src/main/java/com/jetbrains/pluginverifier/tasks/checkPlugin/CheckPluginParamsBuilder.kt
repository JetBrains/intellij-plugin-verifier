package com.jetbrains.pluginverifier.tasks.checkPlugin

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.misc.closeOnException
import com.jetbrains.pluginverifier.misc.tryInvokeSeveralTimes
import com.jetbrains.pluginverifier.options.CmdOpts
import com.jetbrains.pluginverifier.options.OptionsParser
import com.jetbrains.pluginverifier.parameters.jdk.JdkDescriptor
import com.jetbrains.pluginverifier.plugin.PluginCoordinate
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.tasks.TaskParametersBuilder
import java.io.File
import java.util.concurrent.TimeUnit

class CheckPluginParamsBuilder(val pluginRepository: PluginRepository) : TaskParametersBuilder {

  override fun build(opts: CmdOpts, freeArgs: List<String>): CheckPluginParams {
    if (freeArgs.size <= 1) {
      throw IllegalArgumentException("You must specify plugin to check and IDE(s), example:\n" +
          "java -jar verifier.jar check-plugin ~/work/myPlugin/myPlugin.zip ~/EAPs/idea-IU-117.963\n" +
          "java -jar verifier.jar check-plugin #14986 ~/EAPs/idea-IU-117.963")
    }
    val ideDescriptors = freeArgs.drop(1).map { File(it) }.map { OptionsParser.createIdeDescriptor(it, opts) }
    val coordinates = getPluginsToCheck(freeArgs[0], ideDescriptors.map { it.ideVersion })
    val jdkDescriptor = JdkDescriptor(OptionsParser.getJdkDir(opts))
    val externalClassesPrefixes = OptionsParser.getExternalClassesPrefixes(opts)
    val externalClasspath = OptionsParser.getExternalClassPath(opts)
    externalClasspath.closeOnException {
      val problemsFilters = OptionsParser.getProblemsFilters(opts)
      return CheckPluginParams(coordinates, ideDescriptors, jdkDescriptor, externalClassesPrefixes, problemsFilters, externalClasspath)
    }
  }

  private fun getPluginsToCheck(pluginToTestArg: String, ideVersions: List<IdeVersion>? = null): List<PluginCoordinate> {
    if (pluginToTestArg.startsWith("@")) {
      val pluginListFile = File(pluginToTestArg.substring(1))
      val pluginPaths = pluginListFile.readLines()
      return ideVersions!!.flatMap { fetchPlugins(it, pluginListFile, pluginPaths) }
    } else if (pluginToTestArg.matches("#\\d+".toRegex())) {
      val updateId = Integer.parseInt(pluginToTestArg.drop(1))
      val updateInfo = pluginRepository.tryInvokeSeveralTimes(3, 5, TimeUnit.SECONDS, "get update information for update #$updateId") {
        getUpdateInfoById(updateId)
      } ?: throw IllegalArgumentException("Update #$updateId is not found in the Plugin Repository")
      return listOf(PluginCoordinate.ByUpdateInfo(updateInfo, pluginRepository))
    } else {
      val file = File(pluginToTestArg)
      if (!file.exists()) {
        throw IllegalArgumentException("The file $file doesn't exist")
      }
      return listOf(PluginCoordinate.ByFile(file))
    }
  }

  fun fetchPlugins(ideVersion: IdeVersion, pluginListFile: File, pluginPaths: List<String>): List<PluginCoordinate> =
      pluginPaths
          .map(String::trim)
          .filter(String::isNotEmpty)
          .flatMap {
            if (it.startsWith("id:")) {
              downloadPluginBuilds(it.substringAfter("id:"), ideVersion)
            } else {
              var pluginFile = File(it)
              if (!pluginFile.isAbsolute) {
                pluginFile = File(pluginListFile.parentFile, it)
              }
              if (!pluginFile.exists()) {
                throw RuntimeException("Plugin file '" + it + "' specified in '" + pluginListFile.absolutePath + "' doesn't exist")
              }
              listOf(PluginCoordinate.ByFile(pluginFile))
            }
          }

  private fun downloadPluginBuilds(pluginId: String, ideVersion: IdeVersion): List<PluginCoordinate> {
    val allCompatibleUpdatesOfPlugin = pluginRepository.tryInvokeSeveralTimes(3, 5, TimeUnit.SECONDS, "fetch all compatible updates of plugin $pluginId with $ideVersion") {
      getAllCompatibleUpdatesOfPlugin(ideVersion, pluginId)
    }
    return allCompatibleUpdatesOfPlugin.map { PluginCoordinate.ByUpdateInfo(it, pluginRepository) }
  }


}