package com.jetbrains.pluginverifier.configurations

import com.intellij.structure.ide.IdeVersion
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.*
import com.jetbrains.pluginverifier.misc.closeLogged
import com.jetbrains.pluginverifier.repository.FileLock
import com.jetbrains.pluginverifier.repository.IdleFileLock
import com.jetbrains.pluginverifier.repository.RepositoryManager
import com.jetbrains.pluginverifier.utils.CmdOpts
import com.jetbrains.pluginverifier.utils.OptionsUtil
import java.io.File
import java.io.IOException

object CheckPluginParamsParser : ConfigurationParamsParser {

  override fun parse(opts: CmdOpts, freeArgs: List<String>): CheckPluginParams {
    if (freeArgs.size <= 1) {
      System.err.println("You must specify plugin to check and IDE(s), example:\n" +
          "java -jar verifier.jar check-plugin ~/work/myPlugin/myPlugin.zip ~/EAPs/idea-IU-117.963\n" +
          "java -jar verifier.jar check-plugin #14986 ~/EAPs/idea-IU-117.963")
      System.exit(1)
    }
    val ideDescriptors = freeArgs.drop(1).map(::File).map { OptionsUtil.createIdeDescriptor(it, opts) }
    val pluginFileLocks = getPluginFileLocks(freeArgs[0], ideDescriptors.map { it.ideVersion })
    val jdkDescriptor = JdkDescriptor(OptionsUtil.getJdkDir(opts))
    val externalClassesPrefixes = OptionsUtil.getExternalClassesPrefixes(opts)
    val externalClasspath = OptionsUtil.getExternalClassPath(opts)
    val problemsFilter = OptionsUtil.getProblemsFilter(opts)
    val pluginsToCheck = pluginFileLocks.map { PluginDescriptor.ByFileLock(it) }
    return CheckPluginParams(pluginsToCheck, ideDescriptors, jdkDescriptor, externalClassesPrefixes, problemsFilter, externalClasspath)
  }

  private fun getPluginFileLocks(pluginToTestArg: String, ideVersions: List<IdeVersion>? = null): List<FileLock> {
    if (pluginToTestArg.startsWith("@")) {
      val pluginListFile = File(pluginToTestArg.substring(1))
      val pluginPaths = pluginListFile.readLines()
      return ideVersions!!.map { fetchPlugins(it, pluginListFile, pluginPaths) }.flatten()
    } else if (pluginToTestArg.matches("#\\d+".toRegex())) {
      val pluginId = pluginToTestArg.substring(1)
      try {
        val updateId = Integer.parseInt(pluginId)
        val pluginLock = RepositoryManager.getPluginFile(updateId) ?: throw RuntimeException("No such plugin $pluginToTestArg")
        return listOf(pluginLock)
      } catch (e: IOException) {
        throw RuntimeException("Cannot load plugin #" + pluginId, e)
      }
    } else {
      val file = File(pluginToTestArg)
      if (!file.exists()) {
        throw IllegalArgumentException("The file $file doesn't exist")
      }
      return listOf(IdleFileLock(file))
    }
  }

  fun fetchPlugins(ideVersion: IdeVersion, pluginListFile: File, pluginPaths: List<String>): List<FileLock> =
      pluginPaths
          .map(String::trim)
          .filter(String::isNotEmpty)
          .map {
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
              listOf(IdleFileLock(pluginFile))
            }
          }.flatten()

  fun downloadPluginBuilds(pluginId: String, ideVersion: IdeVersion): List<FileLock> =
      RepositoryManager
          .getAllCompatibleUpdatesOfPlugin(ideVersion, pluginId)
          .map { RepositoryManager.getPluginFile(it)!! }


}

data class CheckPluginParams(val pluginDescriptors: List<PluginDescriptor>,
                             val ideDescriptors: List<IdeDescriptor.ByInstance>,
                             val jdkDescriptor: JdkDescriptor,
                             val externalClassesPrefixes: List<String>,
                             val problemsFilter: ProblemsFilter,
                             val externalClasspath: Resolver = Resolver.getEmptyResolver(),
                             val progress: Progress = DefaultProgress()) : ConfigurationParams {

  override fun close() {
    try {
      ideDescriptors.forEach { it.createIdeResult.closeLogged() }
    } finally {
      pluginDescriptors.forEach { (it as? PluginDescriptor.ByFileLock)?.fileLock?.release() }
    }
  }
}
