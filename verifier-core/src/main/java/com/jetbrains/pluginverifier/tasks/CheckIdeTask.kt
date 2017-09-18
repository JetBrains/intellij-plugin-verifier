package com.jetbrains.pluginverifier.tasks

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.api.PluginCoordinate
import com.jetbrains.pluginverifier.api.Progress
import com.jetbrains.pluginverifier.api.VerifierParams
import com.jetbrains.pluginverifier.core.VerifierExecutor
import com.jetbrains.pluginverifier.plugin.CreatePluginResult
import com.jetbrains.pluginverifier.plugin.PluginCreator
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.UpdateInfo

class CheckIdeTask(private val parameters: CheckIdeParams,
                   val pluginRepository: PluginRepository,
                   val pluginCreator: PluginCreator) : Task() {

  override fun execute(progress: Progress): CheckIdeResult {
    val notExcludedPlugins = parameters.pluginsToCheck.filterNot { isExcluded(it) }
    return doExecute(notExcludedPlugins, progress)
  }

  private fun isExcluded(pluginCoordinate: PluginCoordinate): Boolean = when (pluginCoordinate) {
    is PluginCoordinate.ByUpdateInfo -> {
      val updateInfo = pluginCoordinate.updateInfo
      PluginIdAndVersion(updateInfo.pluginId, updateInfo.version) in parameters.excludedPlugins
    }
    is PluginCoordinate.ByFile -> {
      pluginCreator.createPluginByFile(pluginCoordinate.pluginFile).use { createPluginResult ->
        if (createPluginResult is CreatePluginResult.OK) {
          val plugin = createPluginResult.plugin
          return PluginIdAndVersion(plugin.pluginId ?: "", plugin.pluginVersion ?: "") in parameters.excludedPlugins
        }
        return true
      }
    }
  }

  private fun doExecute(notExcludedPlugins: List<PluginCoordinate>, progress: Progress): CheckIdeResult {
    val verifierParams = VerifierParams(parameters.jdkDescriptor, parameters.externalClassesPrefixes, parameters.problemsFilter, parameters.externalClassPath, parameters.dependencyResolver)
    val verifier = VerifierExecutor(verifierParams)
    verifier.use {
      val results = verifier.verify(notExcludedPlugins.map { it to parameters.ideDescriptor }, progress, pluginCreator)
      return CheckIdeResult(parameters.ideDescriptor.ideVersion, results, parameters.excludedPlugins, getMissingUpdatesProblems())
    }
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

