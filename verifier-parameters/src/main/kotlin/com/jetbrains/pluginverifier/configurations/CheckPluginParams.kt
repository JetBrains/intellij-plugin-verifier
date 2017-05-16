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
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException

object CheckPluginParamsParser : ConfigurationParamsParser {

  private val LOG: Logger = LoggerFactory.getLogger(CheckPluginParamsParser::class.java)

  override fun parse(opts: CmdOpts, freeArgs: List<String>): CheckPluginParams {
    if (freeArgs.size <= 1) {
      System.err.println("You must specify plugin to check and IDE(s), example:\n" +
          "java -jar verifier.jar check-plugin ~/work/myPlugin/myPlugin.zip ~/EAPs/idea-IU-117.963\n" +
          "java -jar verifier.jar check-plugin #14986 ~/EAPs/idea-IU-117.963")
      System.exit(1)
    }
    val ideDescriptors = freeArgs.drop(1).map(::File).map { OptionsUtil.createIdeDescriptor(it, opts) }
    val pluginFiles = getPluginFiles(freeArgs[0], ideDescriptors.map { it.ideVersion })
    val jdkDescriptor = JdkDescriptor(OptionsUtil.getJdkDir(opts))
    val externalClassesPrefixes = OptionsUtil.getExternalClassesPrefixes(opts)
    val externalClasspath = OptionsUtil.getExternalClassPath(opts)
    val problemsFilter = OptionsUtil.getProblemsFilter(opts)
    val pluginsToCheck = pluginFiles.map {
      try {
        val plugin = PluginManager.getInstance().createPlugin(it.getFile())
        val pluginResolver = Resolver.createPluginResolver(plugin)
        PluginDescriptor.ByInstance(plugin, pluginResolver)
      } catch (e: Exception) {
        //the plugin is not opened, but we wan't to show a failure result (the verifier will provide a message)
        val pluginIdAndVersion = guessPluginIdAndVersion(it.getFile())
        PluginDescriptor.ByFileLock(pluginIdAndVersion.first, pluginIdAndVersion.second, it)
      }
    }
    return CheckPluginParams(pluginsToCheck, ideDescriptors, jdkDescriptor, externalClassesPrefixes, problemsFilter, externalClasspath)
  }

  private fun guessPluginIdAndVersion(file: File): Pair<String, String> {
    val name = file.nameWithoutExtension
    val version = name.substringAfterLast('-')
    return name.substringBeforeLast('-') to version
  }

  fun getPluginFiles(pluginToTestArg: String, ideVersions: List<IdeVersion>? = null): List<FileLock> {
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

  override fun close() = ideDescriptors.forEach { it.ideResolver.closeLogged() }
}
