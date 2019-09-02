package com.jetbrains.pluginverifier.tasks.deprecatedUsages

import com.jetbrains.plugin.structure.base.utils.isDirectory
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.PluginVerificationDescriptor
import com.jetbrains.pluginverifier.PluginVerificationTarget
import com.jetbrains.pluginverifier.dependencies.resolution.createIdeBundledOrPluginRepositoryDependencyFinder
import com.jetbrains.pluginverifier.options.CmdOpts
import com.jetbrains.pluginverifier.options.OptionsParser
import com.jetbrains.pluginverifier.options.PluginsParsing
import com.jetbrains.pluginverifier.options.PluginsSet
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.reporting.PluginVerificationReportage
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.resolution.DefaultClassResolverProvider
import com.jetbrains.pluginverifier.tasks.TaskParametersBuilder
import com.jetbrains.pluginverifier.verifiers.packages.DefaultPackageFilter
import com.sampullara.cli.Args
import com.sampullara.cli.Argument
import java.nio.file.Paths

class DeprecatedUsagesParamsBuilder(
    private val pluginRepository: PluginRepository,
    private val pluginDetailsCache: PluginDetailsCache,
    private val reportage: PluginVerificationReportage
) : TaskParametersBuilder {
  override fun build(opts: CmdOpts, freeArgs: List<String>): DeprecatedUsagesParams {
    val deprecatedOpts = DeprecatedUsagesOpts()
    val unparsedArgs = Args.parse(deprecatedOpts, freeArgs.toTypedArray(), false)
    require(unparsedArgs.isNotEmpty()) { "You have to specify path to IDE which deprecated API usages are to be found. For example: \"java -jar verifier.jar check-ide ~/EAPs/idea-IU-133.439\"" }
    val idePath = Paths.get(unparsedArgs[0])
    require(idePath.isDirectory) { "IDE path must be a directory: $idePath" }
    val ideDescriptor = OptionsParser.createIdeDescriptor(idePath, opts)
    /**
     * If the release IDE version is specified, get the compatible plugins' versions based on it.
     * Otherwise, use the version of the verified IDE.
     */
    val ideVersion = deprecatedOpts.releaseIdeVersion?.let { IdeVersion.createIdeVersionIfValid(it) }
        ?: ideDescriptor.ideVersion

    val pluginsSet = PluginsSet()
    PluginsParsing(pluginRepository, reportage, pluginsSet).addPluginsFromCmdOpts(opts, ideVersion)

    val dependencyFinder = createIdeBundledOrPluginRepositoryDependencyFinder(ideDescriptor.ide, pluginRepository, pluginDetailsCache)

    val classResolverProvider = DefaultClassResolverProvider(
        dependencyFinder,
        ideDescriptor,
        DefaultPackageFilter(emptyList())
    )

    val verificationDescriptors = pluginsSet.pluginsToCheck.map {
       PluginVerificationDescriptor.IDE(ideDescriptor, classResolverProvider, it)
    }

    val verificationTarget = PluginVerificationTarget.IDE(ideVersion, ideDescriptor.jdkVersion)

    pluginsSet.ignoredPlugins.forEach { (plugin, reason) ->
      reportage.logPluginVerificationIgnored(plugin, verificationTarget, reason)
    }

    return DeprecatedUsagesParams(
        pluginsSet,
        ideDescriptor,
        ideVersion,
        verificationDescriptors
    )
  }

  class DeprecatedUsagesOpts {
    @set:Argument(
        "release-ide-version", alias = "riv", description = "The version of the release IDE for which compatible plugins must be " +
        "downloaded and checked against the specified IDE. This is needed when the specified IDE is a trunk-built IDE for which " +
        "there might not be compatible updates"
    )
    var releaseIdeVersion: String? = null
  }

}