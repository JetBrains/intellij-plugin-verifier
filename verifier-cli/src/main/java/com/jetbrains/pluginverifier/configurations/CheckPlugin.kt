package com.jetbrains.pluginverifier.configurations

import com.intellij.structure.domain.Plugin
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.*
import com.jetbrains.pluginverifier.misc.PluginCache
import com.jetbrains.pluginverifier.output.StreamVPrinter
import com.jetbrains.pluginverifier.repository.RepositoryManager
import com.jetbrains.pluginverifier.utils.Opts
import com.jetbrains.pluginverifier.utils.Util
import com.jetbrains.pluginverifier.utils.VOptionsUtil
import java.io.File
import java.io.IOException
import java.io.PrintStream

object CheckPluginParamsParser : ParamsParser {

  override fun parse(opts: Opts, freeArgs: List<String>): CheckPluginParams {
    if (freeArgs.size <= 1) {
      throw RuntimeException("You must specify plugin to check and IDE, example:\n" +
          "java -jar verifier.jar check-plugin ~/work/myPlugin/myPlugin.zip ~/EAPs/idea-IU-117.963\n" +
          "java -jar verifier.jar check-plugin #14986 ~/EAPs/idea-IU-117.963")
    }
    val pluginFile = getPlugin(freeArgs[0])
    val ideFiles = freeArgs.drop(1).map { File(it) }.map { Util.createIde(it, opts) }.map { IdeDescriptor.ByInstance(it) }
    val jdkDescriptor = JdkDescriptor.ByFile(Util.getJdkDir(opts))
    val vOptions = VOptionsUtil.parseOpts(opts)
    val externalClasspath = Util.getExternalClassPath(opts)
    return CheckPluginParams(pluginFile, ideFiles, jdkDescriptor, vOptions, externalClasspath)
  }

  private fun getPlugin(pluginToTestArg: String): PluginDescriptor {
    if (pluginToTestArg.matches("#\\d+".toRegex())) {
      val pluginId = pluginToTestArg.substring(1)
      try {
        val updateId = Integer.parseInt(pluginId)
        val updateInfo = RepositoryManager.getInstance().findUpdateById(updateId) ?: throw RuntimeException("No such plugin $pluginToTestArg")
        val update = RepositoryManager.getInstance().getPluginFile(updateInfo) ?: throw RuntimeException("No such plugin $pluginToTestArg")
        return PluginDescriptor.ByFile(updateInfo.pluginId ?: updateInfo.pluginName ?: "<unknown>", updateInfo.version ?: "<unknown>", update)
      } catch (e: IOException) {
        throw RuntimeException("Cannot load plugin #" + pluginId, e)
      }
    } else {
      val file = File(pluginToTestArg)
      if (!file.exists()) {
        throw IllegalArgumentException("The file $file doesn't exist")
      }
      val plugin: Plugin
      try {
        plugin = PluginCache.createPlugin(file)
      } catch(e: Exception) {
        throw RuntimeException("The plugin $file is invalid", e)
      }
      return PluginDescriptor.ByInstance(plugin)
    }
  }


}

class CheckPluginParams(val pluginDescriptor: PluginDescriptor,
                        val ideDescriptors: List<IdeDescriptor>,
                        val jdkDescriptor: JdkDescriptor,
                        val vOptions: VOptions,
                        val externalClasspath: Resolver = Resolver.getEmptyResolver()) : Params

class CheckPluginResults(val vResults: VResults) : Results {

  fun printResults(out: PrintStream) = StreamVPrinter(out).printResults(vResults)

}

class CheckPluginConfiguration(val params: CheckPluginParams) : Configuration {

  override fun execute(): CheckPluginResults {
    val pluginsToCheck = params.ideDescriptors.map { params.pluginDescriptor to it }
    val vParams = VParams(params.jdkDescriptor, pluginsToCheck, params.vOptions, params.externalClasspath)
    val vResults = VManager.verify(vParams)

    return CheckPluginResults(vResults)
  }


}
