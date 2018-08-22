package com.jetbrains.pluginverifier.tasks.checkIde

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.VerificationTarget
import com.jetbrains.pluginverifier.dependencies.resolution.IdeDependencyFinder
import com.jetbrains.pluginverifier.misc.closeOnException
import com.jetbrains.pluginverifier.misc.isDirectory
import com.jetbrains.pluginverifier.options.CmdOpts
import com.jetbrains.pluginverifier.options.OptionsParser
import com.jetbrains.pluginverifier.options.PluginsParsing
import com.jetbrains.pluginverifier.options.PluginsSet
import com.jetbrains.pluginverifier.options.filter.ExcludedPluginFilter
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.reporting.verification.Reportage
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.repositories.marketplace.UpdateInfo
import com.jetbrains.pluginverifier.tasks.TaskParametersBuilder
import java.nio.file.Paths

class CheckIdeParamsBuilder(val pluginRepository: PluginRepository,
                            val pluginDetailsCache: PluginDetailsCache,
                            val reportage: Reportage) : TaskParametersBuilder {
  override fun build(opts: CmdOpts, freeArgs: List<String>): CheckIdeParams {
    if (freeArgs.isEmpty()) {
      throw IllegalArgumentException("You have to specify IDE to check. For example: \"java -jar verifier.jar check-ide ~/EAPs/idea-IU-133.439\"")
    }
    val ideFile = Paths.get(freeArgs[0])
    if (!ideFile.isDirectory) {
      throw IllegalArgumentException("IDE path must be a directory: $ideFile")
    }
    reportage.logVerificationStage("Reading classes of IDE $ideFile")
    OptionsParser.createIdeDescriptor(ideFile, opts).closeOnException { ideDescriptor ->
      val externalClassesPackageFilter = OptionsParser.getExternalClassesPackageFilter(opts)
      val problemsFilters = OptionsParser.getProblemsFilters(opts)

      val pluginsSet = PluginsSet()
      PluginsParsing(pluginRepository, reportage, pluginsSet).addByPluginIds(opts, ideDescriptor.ideVersion)

      val excludedPlugins = OptionsParser.parseExcludedPlugins(opts)
      val excludedFilter = ExcludedPluginFilter(excludedPlugins)
      pluginsSet.addPluginFilter(excludedFilter)

      pluginsSet.ignoredPlugins.forEach { plugin, reason ->
        reportage.logPluginVerificationIgnored(plugin, VerificationTarget.Ide(ideDescriptor.ideVersion), reason)
      }

      val missingCompatibleVersionsProblems = findMissingCompatibleVersionsProblems(ideDescriptor.ideVersion, pluginsSet)

      val ideDependencyFinder = IdeDependencyFinder(ideDescriptor.ide, pluginRepository, pluginDetailsCache)
      val jdkPath = OptionsParser.getJdkPath(opts)
      return CheckIdeParams(
          pluginsSet,
          jdkPath,
          ideDescriptor,
          externalClassesPackageFilter,
          problemsFilters,
          ideDependencyFinder,
          missingCompatibleVersionsProblems
      )
    }
  }

  /**
   * For all unique plugins' IDs to be verified determines
   * whether there are versions of these plugins
   * available in the Plugin Repository that are compatible
   * with [ideVersion], and returns [MissingCompatibleVersionProblem]s
   * for plugins IDs that don't have ones.
   */
  private fun findMissingCompatibleVersionsProblems(ideVersion: IdeVersion, pluginsSet: PluginsSet): List<MissingCompatibleVersionProblem> {
    val pluginIds = pluginsSet.pluginsToCheck.map { it.pluginId }.distinct()
    val existingPluginIds = pluginRepository.getLastCompatiblePlugins(ideVersion).map { it.pluginId }

    return (pluginIds - existingPluginIds)
        .map {
          val buildForCommunity = findVersionCompatibleWithCommunityEdition(it, ideVersion) as? UpdateInfo
          if (buildForCommunity != null) {
            val details = "\nNote: there is an update (#" + buildForCommunity.updateId + ") compatible with IDEA Community Edition, " +
                "but the Plugin repository does not offer to install it if you run the IDEA Ultimate."
            MissingCompatibleVersionProblem(it, ideVersion, details)
          } else {
            MissingCompatibleVersionProblem(it, ideVersion, "")
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
      } catch (ie: InterruptedException) {
        throw ie
      } catch (e: Exception) {
        null
      }
    }
    return null
  }

}