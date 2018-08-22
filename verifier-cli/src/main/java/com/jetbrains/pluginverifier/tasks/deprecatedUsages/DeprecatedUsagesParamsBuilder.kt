package com.jetbrains.pluginverifier.tasks.deprecatedUsages

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.VerificationTarget
import com.jetbrains.pluginverifier.dependencies.resolution.IdeDependencyFinder
import com.jetbrains.pluginverifier.misc.isDirectory
import com.jetbrains.pluginverifier.options.CmdOpts
import com.jetbrains.pluginverifier.options.OptionsParser
import com.jetbrains.pluginverifier.options.PluginsParsing
import com.jetbrains.pluginverifier.options.PluginsSet
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.reporting.verification.Reportage
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.tasks.TaskParametersBuilder
import com.sampullara.cli.Args
import com.sampullara.cli.Argument
import java.nio.file.Paths

class DeprecatedUsagesParamsBuilder(
    private val pluginRepository: PluginRepository,
    private val pluginDetailsCache: PluginDetailsCache,
    private val reportage: Reportage
) : TaskParametersBuilder {
  override fun build(opts: CmdOpts, freeArgs: List<String>): DeprecatedUsagesParams {
    val deprecatedOpts = DeprecatedUsagesOpts()
    val unparsedArgs = Args.parse(deprecatedOpts, freeArgs.toTypedArray(), false)
    if (unparsedArgs.isEmpty()) {
      throw IllegalArgumentException("You have to specify path to IDE which deprecated API usages are to be found. For example: \"java -jar verifier.jar check-ide ~/EAPs/idea-IU-133.439\"")
    }
    val idePath = Paths.get(unparsedArgs[0])
    if (!idePath.isDirectory) {
      throw IllegalArgumentException("IDE path must be a directory: $idePath")
    }
    val ideDescriptor = OptionsParser.createIdeDescriptor(idePath, opts)
    /**
     * If the release IDE version is specified, get the compatible plugins' versions based on it.
     * Otherwise, use the version of the verified IDE.
     */
    val ideVersion = deprecatedOpts.releaseIdeVersion?.let { IdeVersion.createIdeVersionIfValid(it) }
        ?: ideDescriptor.ideVersion

    val pluginsSet = PluginsSet()
    PluginsParsing(pluginRepository, reportage, pluginsSet).addByPluginIds(opts, ideVersion)

    pluginsSet.ignoredPlugins.forEach { plugin, reason ->
      reportage.logPluginVerificationIgnored(plugin, VerificationTarget.Ide(ideVersion), reason)
    }

    val dependencyFinder = IdeDependencyFinder(ideDescriptor.ide, pluginRepository, pluginDetailsCache)
    return DeprecatedUsagesParams(
        pluginsSet,
        OptionsParser.getJdkPath(opts),
        ideDescriptor,
        dependencyFinder,
        ideVersion
    )
  }

  class DeprecatedUsagesOpts {
    @set:Argument("release-ide-version", alias = "riv", description = "The version of the release IDE for which compatible plugins must be " +
        "downloaded and checked against the specified IDE. This is needed when the specified IDE is a trunk-built IDE for which " +
        "there might not be compatible updates")
    var releaseIdeVersion: String? = null
  }

}