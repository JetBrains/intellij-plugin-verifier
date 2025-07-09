/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.tasks.checkPlugin

import com.jetbrains.plugin.structure.intellij.plugin.PluginArchiveManager
import com.jetbrains.pluginverifier.PluginVerificationDescriptor
import com.jetbrains.pluginverifier.PluginVerificationTarget
import com.jetbrains.pluginverifier.dependencies.resolution.CompositeDependencyFinder
import com.jetbrains.pluginverifier.dependencies.resolution.DependencyFinder
import com.jetbrains.pluginverifier.dependencies.resolution.LastVersionSelector
import com.jetbrains.pluginverifier.dependencies.resolution.RepositoryDependencyFinder
import com.jetbrains.pluginverifier.dependencies.resolution.createIdeBundledOrPluginRepositoryDependencyFinder
import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.options.CmdOpts
import com.jetbrains.pluginverifier.options.OptionsParser
import com.jetbrains.pluginverifier.options.PluginsParsing
import com.jetbrains.pluginverifier.options.PluginsSet
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.reporting.PluginVerificationReportage
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.repositories.local.LocalPluginRepository
import com.jetbrains.pluginverifier.resolution.DefaultClassResolverProvider
import com.jetbrains.pluginverifier.tasks.TaskParametersBuilder
import java.nio.file.Paths

const val JETBRAINS_PLUGINS_API_USAGE_MODE = "jetbrains-plugins"
const val STANDARD_API_USAGE_MODE = "no"

class CheckPluginParamsBuilder(
  val pluginRepository: PluginRepository,
  val reportage: PluginVerificationReportage,
  val pluginDetailsCache: PluginDetailsCache,
  val archiveManager: PluginArchiveManager,
  private val ideDescriptorParser: IdeDescriptorParser = DefaultIdeDescriptorParser(reportage)
) : TaskParametersBuilder {

  override fun build(opts: CmdOpts, freeArgs: List<String>): CheckPluginParams {
    val internalApiVerificationMode = OptionsParser.parseInternalApiVerificationMode(opts)
    require(freeArgs.size > 1) {
      "You must specify plugin to check and IDE(s), example:\n" +
        "java -jar verifier.jar check-plugin ~/work/myPlugin/myPlugin.zip ~/EAPs/idea-IU-117.963\n" +
        "java -jar verifier.jar check-plugin #14986 ~/EAPs/idea-IU-117.963"
    }

    val ideDescriptors = ideDescriptorParser.parseIdeDescriptors(opts, freeArgs)
    val ideVersions = ideDescriptors.map { it.ideVersion }
    val pluginParsingConfiguration = OptionsParser.createPluginParsingConfiguration(opts)
    val pluginsSet = PluginsSet()
    // pluginsParsing will modify [pluginsSet] in-place.
    val pluginsParsing = PluginsParsing(pluginRepository, archiveManager, reportage, pluginsSet, pluginParsingConfiguration)

    val pluginToTestArg = freeArgs[0]
    when {
      pluginToTestArg.startsWith("@") -> {
        pluginsParsing.addPluginsListedInFile(
          Paths.get(pluginToTestArg.substringAfter("@")),
          ideVersions
        )
      }
      else -> {
        pluginsParsing.addPluginBySpec(pluginToTestArg, Paths.get(""), ideVersions)
      }
    }

    val externalClassesPackageFilter = OptionsParser.getExternalClassesPackageFilter(opts)
    val problemsFilters = OptionsParser.getProblemsFilters(opts)

    val verificationDescriptors = ideDescriptors.flatMap { ideDescriptor ->
      val dependencyFinder = createDependencyFinder(pluginsSet.localRepository, ideDescriptor, pluginDetailsCache)
      val classResolverProvider = DefaultClassResolverProvider(
        dependencyFinder,
        ideDescriptor,
        externalClassesPackageFilter,
        downloadUnavailableBundledPlugins = true,
        archiveManager = archiveManager
      )

      pluginsSet.pluginsToCheck.map {
        PluginVerificationDescriptor.IDE(ideDescriptor, classResolverProvider, it)
      }
    }

    val verificationTargets = ideDescriptors.map {
      PluginVerificationTarget.IDE(it.ideVersion, it.jdkVersion)
    }

    pluginsSet.ignoredPlugins.forEach { (plugin, reason) ->
      verificationTargets.forEach { verificationTarget ->
        reportage.logPluginVerificationIgnored(plugin, verificationTarget, reason)
      }
    }

    return CheckPluginParams(
      ideDescriptors,
      problemsFilters,
      verificationDescriptors,
      pluginsSet.invalidPluginFiles,
      opts.excludeExternalBuildClassesSelector,
      internalApiVerificationMode
    )
  }

  /**
   * Creates the [DependencyFinder] that firstly tries to resolve the dependency among the verified plugins.
   *
   * The 'check-plugin' task searches for dependencies among the verified plugins:
   * suppose plugins A and B are verified simultaneously and A depends on B.
   * Then B must be resolved to the local plugin when the A is verified.
   */
  private fun createDependencyFinder(localRepository: LocalPluginRepository, ideDescriptor: IdeDescriptor, pluginDetailsCache: PluginDetailsCache): DependencyFinder {
    val localFinder = RepositoryDependencyFinder(localRepository, LastVersionSelector(), pluginDetailsCache)
    val ideDependencyFinder = createIdeBundledOrPluginRepositoryDependencyFinder(ideDescriptor.ide, pluginRepository, pluginDetailsCache)
    return CompositeDependencyFinder(listOf(localFinder, ideDependencyFinder))
  }
}

fun interface IdeDescriptorParser {
  fun parseIdeDescriptors(opts: CmdOpts, freeArgs: List<String>): List<IdeDescriptor>
}

class DefaultIdeDescriptorParser(private val reportage: PluginVerificationReportage,) : IdeDescriptorParser {
  override fun parseIdeDescriptors(opts: CmdOpts, freeArgs: List<String>): List<IdeDescriptor> {
    return freeArgs.drop(1).map {
      reportage.logVerificationStage("Reading IDE $it")
      OptionsParser.createIdeDescriptor(it, opts)
    }
  }
}
