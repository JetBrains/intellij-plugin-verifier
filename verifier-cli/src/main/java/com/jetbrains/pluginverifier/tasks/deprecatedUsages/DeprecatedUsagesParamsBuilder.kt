package com.jetbrains.pluginverifier.tasks.deprecatedUsages

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.dependencies.resolution.IdeDependencyFinder
import com.jetbrains.pluginverifier.misc.isDirectory
import com.jetbrains.pluginverifier.options.CmdOpts
import com.jetbrains.pluginverifier.options.OptionsParser
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.UpdateInfo
import com.jetbrains.pluginverifier.tasks.TaskParametersBuilder
import com.sampullara.cli.Args
import com.sampullara.cli.Argument
import java.nio.file.Paths

class DeprecatedUsagesParamsBuilder(val pluginRepository: PluginRepository,
                                    val pluginDetailsCache: PluginDetailsCache) : TaskParametersBuilder {
  override fun build(opts: CmdOpts, freeArgs: List<String>): DeprecatedUsagesParams {
    val deprecatedOpts = DeprecatedUsagesOpts()
    val unparsedArgs = Args.parse(deprecatedOpts, freeArgs.toTypedArray(), false)
    if (unparsedArgs.isEmpty()) {
      throw IllegalArgumentException("You have to specify path to IDE which deprecated API usages are to be found. For example: \"java -jar verifier.jar check-ide ~/EAPs/idea-IU-133.439\"")
    }
    val idePath = Paths.get(unparsedArgs[0])
    if (!idePath.isDirectory) {
      throw IllegalArgumentException("IDE path must be a directory: " + idePath)
    }
    val ideDescriptor = OptionsParser.createIdeDescriptor(idePath, opts)
    val jdkDescriptor = OptionsParser.createJdkDescriptor(opts)
    /**
     * If the release IDE version is specified, get the compatible plugins' versions based on it.
     * Otherwise, use the version of the verified IDE.
     */
    val ideVersionForCompatiblePlugins = deprecatedOpts.releaseIdeVersion?.let { IdeVersion.createIdeVersionIfValid(it) } ?: ideDescriptor.ideVersion
    val updatesToCheck = requestUpdatesToCheck(opts, ideVersionForCompatiblePlugins)
    val pluginInfos = updatesToCheck.map { it as UpdateInfo }
    val dependencyFinder = IdeDependencyFinder(ideDescriptor.ide, pluginRepository, pluginDetailsCache)
    return DeprecatedUsagesParams(ideDescriptor, jdkDescriptor, pluginInfos, dependencyFinder, ideVersionForCompatiblePlugins)
  }

  private fun requestUpdatesToCheck(allOpts: CmdOpts, ideVersionForCompatiblePlugins: IdeVersion): List<PluginInfo> {
    val (checkAllBuilds, checkLastBuilds) = OptionsParser.parsePluginsToCheck(allOpts)
    return OptionsParser.requestUpdatesToCheckByIds(checkAllBuilds, checkLastBuilds, ideVersionForCompatiblePlugins, pluginRepository)
  }

  class DeprecatedUsagesOpts {
    @set:Argument("release-ide-version", alias = "riv", description = "The version of the release IDE for which compatible plugins must be " +
        "downloaded and checked against the specified IDE. This is needed when the specified IDE is a trunk-built IDE for which " +
        "there might not be compatible updates")
    var releaseIdeVersion: String? = null
  }

}