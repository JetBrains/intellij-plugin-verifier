/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.tasks.checkIde

import com.jetbrains.plugin.structure.base.utils.closeOnException
import com.jetbrains.plugin.structure.base.utils.rethrowIfInterrupted
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.PluginVerificationDescriptor
import com.jetbrains.pluginverifier.PluginVerificationTarget
import com.jetbrains.pluginverifier.dependencies.resolution.createIdeBundledOrPluginRepositoryDependencyFinder
import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.options.CmdOpts
import com.jetbrains.pluginverifier.options.OptionsParser
import com.jetbrains.pluginverifier.options.PluginsParsing
import com.jetbrains.pluginverifier.options.PluginsSet
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.reporting.PluginVerificationReportage
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.repositories.marketplace.UpdateInfo
import com.jetbrains.pluginverifier.resolution.DefaultClassResolverProvider
import com.jetbrains.pluginverifier.tasks.TaskParametersBuilder

class CheckIdeParamsBuilder(
  val pluginRepository: PluginRepository,
  val pluginDetailsCache: PluginDetailsCache,
  val reportage: PluginVerificationReportage
) : TaskParametersBuilder {

  override fun build(opts: CmdOpts, freeArgs: List<String>): CheckIdeParams {
    require(freeArgs.isNotEmpty()) { "You have to specify IDE to check. For example: \"java -jar verifier.jar check-ide ~/EAPs/idea-IU-133.439\"" }
    OptionsParser.createIdeDescriptor(freeArgs[0], opts).closeOnException { ideDescriptor: IdeDescriptor ->
      val externalClassesPackageFilter = OptionsParser.getExternalClassesPackageFilter(opts)
      val problemsFilters = OptionsParser.getProblemsFilters(opts)

      val pluginsSet = PluginsSet()
      PluginsParsing(pluginRepository, reportage, pluginsSet).addPluginsFromCmdOpts(opts, ideDescriptor.ideVersion)

      val missingCompatibleVersionsProblems = findMissingCompatibleVersionsProblems(ideDescriptor.ideVersion, pluginsSet)

      val dependencyFinder = createIdeBundledOrPluginRepositoryDependencyFinder(ideDescriptor.ide, pluginRepository, pluginDetailsCache)

      val classResolverProvider = DefaultClassResolverProvider(
        dependencyFinder,
        ideDescriptor,
        externalClassesPackageFilter
      )

      val verificationDescriptors = pluginsSet.pluginsToCheck.map {
        PluginVerificationDescriptor.IDE(ideDescriptor, classResolverProvider, it)
      }

      val verificationTarget = PluginVerificationTarget.IDE(ideDescriptor.ideVersion, ideDescriptor.jdkVersion)
      pluginsSet.ignoredPlugins.forEach { (plugin, reason) ->
        reportage.logPluginVerificationIgnored(plugin, verificationTarget, reason)
      }

      return CheckIdeParams(
        verificationTarget,
        verificationDescriptors,
        problemsFilters,
        missingCompatibleVersionsProblems,
        ideDescriptor,
        opts.excludeExternalBuildClassesSelector
      )
    }
  }

  /**
   * For all unique plugins' IDs to be verified determines
   * whether there are versions of these plugins
   * available in JetBrains Marketplace that are compatible
   * with [ideVersion], and returns [MissingCompatibleVersionProblem]s
   * for plugins IDs that don't have ones.
   */
  private fun findMissingCompatibleVersionsProblems(ideVersion: IdeVersion, pluginsSet: PluginsSet): List<MissingCompatibleVersionProblem> {
    val pluginIds = pluginsSet.pluginsToCheck.map { it.pluginId }.distinct()
    val existingPluginIds = runCatching {
      pluginRepository.getLastCompatiblePlugins(ideVersion).map { it.pluginId }
    }.getOrDefault(emptyList())

    return (pluginIds - existingPluginIds)
      .map {
        val buildForCommunity = findVersionCompatibleWithCommunityEdition(it, ideVersion) as? UpdateInfo
        if (buildForCommunity != null) {
          val details = "\nNote: there is an update (#" + buildForCommunity.updateId + ") compatible with IDEA Community Edition, " +
            "but JetBrains Marketplace does not offer to install it if you run the IDEA Ultimate."
          MissingCompatibleVersionProblem(it, ideVersion, details)
        } else {
          MissingCompatibleVersionProblem(it, ideVersion, null)
        }
      }
  }

  private fun findVersionCompatibleWithCommunityEdition(pluginId: String, version: IdeVersion): PluginInfo? {
    val asString = version.asString()
    if (asString.startsWith("IU-")) {
      val communityVersion = "IC-" + asString.substringAfter(asString, "IU-")
      return try {
        val ideVersion = IdeVersion.createIdeVersion(communityVersion)
        pluginRepository.getLastCompatibleVersionOfPlugin(ideVersion, pluginId)
      } catch (e: Exception) {
        e.rethrowIfInterrupted()
        null
      }
    }
    return null
  }

}
