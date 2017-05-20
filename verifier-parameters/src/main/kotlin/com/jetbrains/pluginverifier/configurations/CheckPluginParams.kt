package com.jetbrains.pluginverifier.configurations

import com.intellij.structure.ide.IdeVersion
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.*
import com.jetbrains.pluginverifier.misc.closeLogged
import com.jetbrains.pluginverifier.misc.closeOnException
import com.jetbrains.pluginverifier.repository.FileLock
import com.jetbrains.pluginverifier.repository.IdleFileLock
import com.jetbrains.pluginverifier.repository.RepositoryManager
import com.jetbrains.pluginverifier.utils.CmdOpts
import com.jetbrains.pluginverifier.utils.OptionsUtil
import java.io.File

object CheckPluginParamsParser : ConfigurationParamsParser {

  override fun parse(opts: CmdOpts, freeArgs: List<String>): CheckPluginParams {
    if (freeArgs.size <= 1) {
      System.err.println("You must specify plugin to check and IDE(s), example:\n" +
          "java -jar verifier.jar check-plugin ~/work/myPlugin/myPlugin.zip ~/EAPs/idea-IU-117.963\n" +
          "java -jar verifier.jar check-plugin #14986 ~/EAPs/idea-IU-117.963")
      System.exit(1)
    }
    val ideDescriptors = freeArgs.drop(1).map(::File).map { OptionsUtil.createIdeDescriptor(it, opts) }
    val pluginDescriptors = getPluginDescriptorsToCheck(freeArgs[0], ideDescriptors.map { it.ideVersion })
    pluginDescriptors.closeOnException {
      val jdkDescriptor = JdkDescriptor(OptionsUtil.getJdkDir(opts))
      val externalClassesPrefixes = OptionsUtil.getExternalClassesPrefixes(opts)
      val externalClasspath = OptionsUtil.getExternalClassPath(opts)
      externalClasspath.closeOnException {
        val problemsFilter = OptionsUtil.getProblemsFilter(opts)
        return CheckPluginParams(pluginDescriptors, ideDescriptors, jdkDescriptor, externalClassesPrefixes, problemsFilter, externalClasspath)
      }
    }
  }

  private fun getPluginDescriptorsToCheck(pluginToTestArg: String, ideVersions: List<IdeVersion>? = null): List<PluginDescriptor> {
    if (pluginToTestArg.startsWith("@")) {
      val pluginListFile = File(pluginToTestArg.substring(1))
      val pluginPaths = pluginListFile.readLines()
      return ideVersions!!.map { fetchPlugins(it, pluginListFile, pluginPaths) }.flatten().map { PluginDescriptor.ByFileLock(it) }
    } else if (pluginToTestArg.matches("#\\d+".toRegex())) {
      val updateId = Integer.parseInt(pluginToTestArg.drop(1))
      val updateInfo = RepositoryManager.getUpdateInfoById(updateId) ?: throw IllegalArgumentException("Update #$updateId is not found in the Plugin Repository")
      return listOf(PluginDescriptor.ByUpdateInfo(updateInfo))
    } else {
      val file = File(pluginToTestArg)
      if (!file.exists()) {
        throw IllegalArgumentException("The file $file doesn't exist")
      }
      return listOf(PluginDescriptor.ByFileLock(IdleFileLock(file)))
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
                             val ideDescriptors: List<IdeDescriptor>,
                             val jdkDescriptor: JdkDescriptor,
                             val externalClassesPrefixes: List<String>,
                             val problemsFilter: ProblemsFilter,
                             val externalClasspath: Resolver = Resolver.getEmptyResolver(),
                             val progress: Progress = DefaultProgress()) : ConfigurationParams {

  override fun presentableText(): String = """Check Plugin Configuration parameters:
  JDK: $jdkDescriptor
  Plugins to be checked: [${pluginDescriptors.joinToString()}]
  IDE builds to be checked: [${ideDescriptors.joinToString()}]
  External classes prefixes: [${externalClassesPrefixes.joinToString()}]
  """

  override fun close() {
    try {
      ideDescriptors.forEach { it.createIdeResult.closeLogged() }
    } finally {
      pluginDescriptors.forEach { (it as? PluginDescriptor.ByFileLock)?.fileLock?.release() }
    }
  }

  override fun toString(): String = presentableText()
}
