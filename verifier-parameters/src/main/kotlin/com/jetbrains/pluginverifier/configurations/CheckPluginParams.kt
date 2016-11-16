package com.jetbrains.pluginverifier.configurations

import com.intellij.structure.domain.IdeVersion
import com.intellij.structure.domain.PluginManager
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.*
import com.jetbrains.pluginverifier.repository.IFileLock
import com.jetbrains.pluginverifier.repository.IdleFileLock
import com.jetbrains.pluginverifier.repository.RepositoryManager
import com.jetbrains.pluginverifier.utils.CmdOpts
import com.jetbrains.pluginverifier.utils.CmdUtil
import com.jetbrains.pluginverifier.utils.VOptionsUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException

object CheckPluginParamsParser : ParamsParser {

  private val LOG: Logger = LoggerFactory.getLogger(CheckPluginParamsParser::class.java)

  override fun parse(opts: CmdOpts, freeArgs: List<String>): CheckPluginParams {
    if (freeArgs.size <= 1) {
      System.err.println("You must specify plugin to check and IDE(s), example:\n" +
          "java -jar verifier.jar check-plugin ~/work/myPlugin/myPlugin.zip ~/EAPs/idea-IU-117.963\n" +
          "java -jar verifier.jar check-plugin #14986 ~/EAPs/idea-IU-117.963")
      System.exit(1)
    }
    val ideDescriptors = freeArgs.drop(1).map { File(it) }.map { IdeDescriptor.ByInstance(CmdUtil.createIde(it, opts)) }
    val pluginFiles = getPluginFiles(freeArgs[0], ideDescriptors.map { it.ideVersion })
    val jdkDescriptor = JdkDescriptor.ByFile(CmdUtil.getJdkDir(opts))
    val vOptions = VOptionsUtil.parseOpts(opts)
    val externalClasspath = CmdUtil.getExternalClassPath(opts)
    val pluginsToCheck = pluginFiles.map {
      try {
        val plugin = PluginManager.getInstance().createPlugin(it.getFile())
        PluginDescriptor.ByInstance(plugin)
      } catch(e: Exception) {
        val (id, version) = guessPluginIdAndVersion(it.getFile())
        LOG.debug("Unable to create plugin for ${it.getFile()}; supposed (id; version) = ($id; $version)", e)
        PluginDescriptor.ByFileLock(id, version, it)
      }
    }
    return CheckPluginParams(pluginsToCheck, ideDescriptors, jdkDescriptor, vOptions, true, externalClasspath)
  }

  private fun guessPluginIdAndVersion(file: File): Pair<String, String> {
    val name = file.nameWithoutExtension
    val version = name.substringAfterLast('-')
    return name.substringBeforeLast('-') to version
  }

  fun getPluginFiles(pluginToTestArg: String, ideVersions: List<IdeVersion>? = null): List<IFileLock> {
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

  fun fetchPlugins(ideVersion: IdeVersion, pluginListFile: File, pluginPaths: List<String>): List<IFileLock> =
      pluginPaths
          .map { it.trim() }
          .filter { it.isNotEmpty() }
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

  fun downloadPluginBuilds(pluginId: String, ideVersion: IdeVersion): List<IFileLock> =
      RepositoryManager
          .getAllCompatibleUpdatesOfPlugin(ideVersion, pluginId)
          .map { RepositoryManager.getPluginFile(it)!! }


}

data class CheckPluginParams(val pluginDescriptors: List<PluginDescriptor>,
                             val ideDescriptors: List<IdeDescriptor>,
                             val jdkDescriptor: JdkDescriptor,
                             val vOptions: VOptions,
                             val resolveDependenciesWithin: Boolean = false,
                             val externalClasspath: Resolver = Resolver.getEmptyResolver(),
                             val progress: VProgress = DefaultVProgress()) : Params
