package com.jetbrains.pluginverifier.tasks.checkPlugin

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.misc.exists
import com.jetbrains.pluginverifier.misc.tryInvokeSeveralTimes
import com.jetbrains.pluginverifier.options.CmdOpts
import com.jetbrains.pluginverifier.options.OptionsParser
import com.jetbrains.pluginverifier.options.PluginsSet
import com.jetbrains.pluginverifier.parameters.jdk.JdkDescriptorsCache
import com.jetbrains.pluginverifier.reporting.verification.VerificationReportage
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.PublicPluginRepository
import com.jetbrains.pluginverifier.repository.UpdateInfo
import com.jetbrains.pluginverifier.repository.local.LocalPluginRepository
import com.jetbrains.pluginverifier.tasks.InvalidPluginFile
import com.jetbrains.pluginverifier.tasks.TaskParametersBuilder
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

class CheckPluginParamsBuilder(val pluginRepository: PluginRepository,
                               val verificationReportage: VerificationReportage) : TaskParametersBuilder {

  override fun build(opts: CmdOpts, freeArgs: List<String>): CheckPluginParams {
    if (freeArgs.size <= 1) {
      throw IllegalArgumentException("You must specify plugin to check and IDE(s), example:\n" +
          "java -jar verifier.jar check-plugin ~/work/myPlugin/myPlugin.zip ~/EAPs/idea-IU-117.963\n" +
          "java -jar verifier.jar check-plugin #14986 ~/EAPs/idea-IU-117.963")
    }
    val ideDescriptors = freeArgs.drop(1).map { Paths.get(it) }.map {
      verificationReportage.logVerificationStage("Reading IDE $it")
      OptionsParser.createIdeDescriptor(it, opts)
    }

    val pluginsSet = PluginsSet()
    val invalidPluginFiles = arrayListOf<InvalidPluginFile>()
    val pluginToTestArg = freeArgs[0]
    parsePluginsToCheckSet(pluginsSet, invalidPluginFiles, pluginToTestArg, ideDescriptors.map { it.ideVersion })

    pluginsSet.ignoredPlugins.forEach { plugin, reason ->
      ideDescriptors.map { it.ideVersion }.forEach { ideVersion ->
        verificationReportage.logPluginVerificationIgnored(plugin, ideVersion, reason)
      }
    }

    val jdkDescriptorsCache = JdkDescriptorsCache()
    val externalClassesPrefixes = OptionsParser.getExternalClassesPrefixes(opts)
    val problemsFilters = OptionsParser.getProblemsFilters(opts)
    return CheckPluginParams(
        pluginsSet,
        OptionsParser.getJdkPath(opts),
        ideDescriptors,
        jdkDescriptorsCache,
        externalClassesPrefixes,
        problemsFilters,
        invalidPluginFiles
    )
  }

  //todo: move to [OptionsParser] and perform refactoring
  private fun parsePluginsToCheckSet(pluginsSet: PluginsSet,
                                     invalidPluginFiles: MutableList<InvalidPluginFile>,
                                     pluginToTestArg: String,
                                     ideVersions: List<IdeVersion>) {
    verificationReportage.logVerificationStage("Parse a list of plugins to check")
    when {
      pluginToTestArg.startsWith("@") -> {
        schedulePluginsFromFile(
            pluginsSet,
            invalidPluginFiles,
            File(pluginToTestArg.substringAfter("@")),
            ideVersions
        )
      }
      pluginToTestArg.matches("#\\d+".toRegex()) -> {
        val updateId = Integer.parseInt(pluginToTestArg.drop(1))
        val updateInfo = pluginRepository.tryInvokeSeveralTimes(3, 5, TimeUnit.SECONDS, "get update information for update #$updateId") {
          (this as? PublicPluginRepository)?.getPluginInfoById(updateId)
        } ?: throw IllegalArgumentException("Update #$updateId is not found in the Plugin Repository")
        pluginsSet.schedulePlugin(updateInfo)
      }
      else -> {
        val file = Paths.get(pluginToTestArg)
        if (!file.exists()) {
          throw IllegalArgumentException("The file $file doesn't exist")
        }
        addPluginToCheckFromFile(file, LocalPluginRepository(file.toUri().toURL()), pluginsSet, invalidPluginFiles)
      }
    }
  }

  private fun schedulePluginsFromFile(pluginsSet: PluginsSet,
                                      invalidPluginsFiles: MutableList<InvalidPluginFile>,
                                      pluginsListFile: File,
                                      ideVersions: List<IdeVersion>) {
    val pluginPaths = pluginsListFile.readLines().map { it.trim() }.filterNot { it.isEmpty() }
    val localPluginRepository = LocalPluginRepository(pluginsListFile.toURI().toURL())
    for (ideVersion in ideVersions) {
      for (path in pluginPaths) {
        if (path.startsWith("id:")) {
          pluginsSet.schedulePlugins(getCompatiblePluginVersions(path.substringAfter("id:"), ideVersion))
        } else if (path.startsWith("#")) {
          val updateId = path.substringAfter("#").toIntOrNull() ?: continue
          val pluginInfo = getPluginInfoByUpdateId(updateId) ?: continue
          pluginsSet.schedulePlugin(pluginInfo)
        } else {
          var pluginFile = Paths.get(path)
          if (!pluginFile.isAbsolute) {
            pluginFile = pluginsListFile.toPath().resolve(path)
          }
          if (!pluginFile.exists()) {
            throw RuntimeException("Plugin file '" + path + "' specified in '" + pluginsListFile.absolutePath + "' doesn't exist")
          }
          verificationReportage.logVerificationStage("Reading descriptor of a plugin to check from $pluginFile")
          addPluginToCheckFromFile(pluginFile, localPluginRepository, pluginsSet, invalidPluginsFiles)
        }
      }
    }
  }

  private fun addPluginToCheckFromFile(pluginFile: Path,
                                       localPluginRepository: LocalPluginRepository,
                                       pluginsSet: PluginsSet,
                                       invalidPluginFiles: MutableList<InvalidPluginFile>) {
    with(IdePluginManager.createManager().createPlugin(pluginFile.toFile())) {
      when (this) {
        is PluginCreationSuccess -> {
          val localPluginInfo = localPluginRepository.addLocalPlugin(plugin)
          pluginsSet.schedulePlugin(localPluginInfo)
        }
        is PluginCreationFail -> {
          verificationReportage.logVerificationStage("Plugin is invalid in $pluginFile: ${errorsAndWarnings.joinToString()}")
          invalidPluginFiles.add(InvalidPluginFile(pluginFile, errorsAndWarnings))
        }
      }
    }
  }

  private fun getPluginInfoByUpdateId(updateId: Int): PluginInfo? =
      pluginRepository.tryInvokeSeveralTimes(3, 5, TimeUnit.SECONDS, "fetch plugin info for #$updateId") {
        (pluginRepository as? PublicPluginRepository)?.getPluginInfoById(updateId)
      }

  private fun getCompatiblePluginVersions(pluginId: String, ideVersion: IdeVersion): List<PluginInfo> {
    val allCompatibleUpdatesOfPlugin = pluginRepository.tryInvokeSeveralTimes(3, 5, TimeUnit.SECONDS, "fetch all compatible updates of plugin $pluginId with $ideVersion") {
      getAllCompatibleVersionsOfPlugin(ideVersion, pluginId)
    }
    return allCompatibleUpdatesOfPlugin.map { it as UpdateInfo }
  }


}