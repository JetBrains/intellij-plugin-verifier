package com.jetbrains.pluginverifier.tasks.checkIde

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.core.Verification
import com.jetbrains.pluginverifier.logging.VerificationLogger
import com.jetbrains.pluginverifier.parameters.VerifierParameters
import com.jetbrains.pluginverifier.plugin.PluginCoordinate
import com.jetbrains.pluginverifier.plugin.PluginDetailsProvider
import com.jetbrains.pluginverifier.repository.PluginIdAndVersion
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.UpdateInfo
import com.jetbrains.pluginverifier.tasks.Task

class CheckIdeTask(private val parameters: CheckIdeParams,
                   val pluginRepository: PluginRepository,
                   val pluginDetailsProvider: PluginDetailsProvider) : Task() {

  override fun execute(logger: VerificationLogger): CheckIdeResult {
    val notExcludedPlugins = parameters.pluginsToCheck.filterNot { isExcluded(it) }
    return doExecute(notExcludedPlugins, logger)
  }

  private fun isExcluded(pluginCoordinate: PluginCoordinate) = when (pluginCoordinate) {
    is PluginCoordinate.ByUpdateInfo -> {
      val updateInfo = pluginCoordinate.updateInfo
      PluginIdAndVersion(updateInfo.pluginId, updateInfo.version) in parameters.excludedPlugins
    }
    is PluginCoordinate.ByFile -> {
      pluginDetailsProvider.fetchPluginDetails(pluginCoordinate).use { pluginDetails ->
        val plugin = pluginDetails.plugin
        if (plugin != null) {
          return PluginIdAndVersion(plugin.pluginId ?: "", plugin.pluginVersion ?: "") in parameters.excludedPlugins
        }
        return true
      }
    }
  }

  private fun doExecute(notExcludedPlugins: List<PluginCoordinate>, logger: VerificationLogger): CheckIdeResult {
    val verifierParams = VerifierParameters(parameters.jdkDescriptor, parameters.externalClassesPrefixes, parameters.problemsFilters, parameters.externalClassPath, parameters.dependencyFinder)
    val tasks = notExcludedPlugins.map { it to parameters.ideDescriptor }
    val results = Verification.run(verifierParams, pluginDetailsProvider, tasks, logger)
    return CheckIdeResult(parameters.ideDescriptor.ideVersion, results, parameters.excludedPlugins, getMissingUpdatesProblems())
  }

  private fun getMissingUpdatesProblems(): List<MissingCompatibleUpdate> {
    val ideVersion = parameters.ideDescriptor.ideVersion
    val existingUpdatesForIde = pluginRepository.getLastCompatibleUpdates(ideVersion)
        .filterNot { PluginIdAndVersion(it.pluginId, it.version) in parameters.excludedPlugins }
        .map { it.pluginId }
        .toSet()

    return parameters.pluginIdsToCheckExistingBuilds.distinct()
        .filterNot { existingUpdatesForIde.contains(it) }
        .map {
          val buildForCommunity = getUpdateCompatibleWithCommunityEdition(it, ideVersion)
          if (buildForCommunity != null) {
            val details = "\nNote: there is an update (#" + buildForCommunity.updateId + ") compatible with IDEA Community Edition, " +
                "but the Plugin repository does not offer to install it if you run the IDEA Ultimate."
            MissingCompatibleUpdate(it, ideVersion, details)
          } else {
            MissingCompatibleUpdate(it, ideVersion, "")
          }
        }
  }

  private fun getUpdateCompatibleWithCommunityEdition(pluginId: String, version: IdeVersion): UpdateInfo? {
    val ideVersion = version.asString()
    if (ideVersion.startsWith("IU-")) {
      val communityVersion = "IC-" + ideVersion.substringAfter(ideVersion, "IU-")
      return try {
        pluginRepository.getLastCompatibleUpdateOfPlugin(IdeVersion.createIdeVersion(communityVersion), pluginId)
      } catch (e: Exception) {
        null
      }
    }
    return null
  }


}

