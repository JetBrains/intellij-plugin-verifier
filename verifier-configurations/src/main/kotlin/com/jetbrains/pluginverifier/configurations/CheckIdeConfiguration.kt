package com.jetbrains.pluginverifier.configurations

import com.intellij.structure.ide.IdeVersion
import com.jetbrains.pluginverifier.api.PluginCoordinate
import com.jetbrains.pluginverifier.api.VerifierParams
import com.jetbrains.pluginverifier.core.VerifierExecutor
import com.jetbrains.pluginverifier.plugin.CreatePluginResult
import com.jetbrains.pluginverifier.plugin.PluginCreator
import com.jetbrains.pluginverifier.repository.RepositoryManager
import com.jetbrains.pluginverifier.repository.UpdateInfo


class CheckIdeConfiguration : Configuration<CheckIdeParams, CheckIdeResults> {

  private lateinit var params: CheckIdeParams

  override fun execute(parameters: CheckIdeParams): CheckIdeResults {
    params = parameters
    val notExcludedPlugins = parameters.pluginsToCheck.filterNot { isExcluded(it) }
    return doExecute(notExcludedPlugins)
  }

  private fun isExcluded(pluginCoordinate: PluginCoordinate): Boolean = when (pluginCoordinate) {
    is PluginCoordinate.ByUpdateInfo -> {
      val updateInfo = pluginCoordinate.updateInfo
      PluginIdAndVersion(updateInfo.pluginId, updateInfo.version) in params.excludedPlugins
    }
    is PluginCoordinate.ByFile -> {
      PluginCreator.createPluginByFile(pluginCoordinate.pluginFile).use { createPluginResult ->
        if (createPluginResult is CreatePluginResult.OK) {
          val plugin = createPluginResult.plugin
          return PluginIdAndVersion(plugin.pluginId ?: "", plugin.pluginVersion ?: "") in params.excludedPlugins
        }
        return true
      }
    }
  }

  private fun doExecute(notExcludedPlugins: List<PluginCoordinate>): CheckIdeResults {
    val verifierParams = VerifierParams(params.jdkDescriptor, params.externalClassesPrefixes, params.problemsFilter, params.externalClassPath, params.dependencyResolver)
    val verifier = VerifierExecutor(verifierParams)
    verifier.use {
      val results = verifier.verify(notExcludedPlugins.map { it to params.ideDescriptor }, params.progress)
      return CheckIdeResults(params.ideDescriptor.ideVersion, results, params.excludedPlugins, getMissingUpdatesProblems())
    }
  }

  private fun getMissingUpdatesProblems(): List<MissingCompatibleUpdate> {
    val ideVersion = params.ideDescriptor.ideVersion
    val existingUpdatesForIde = RepositoryManager
        .getLastCompatibleUpdates(ideVersion)
        .filterNot { PluginIdAndVersion(it.pluginId, it.version) in params.excludedPlugins }
        .mapNotNull { it.pluginId }
        .toSet()

    return params.pluginIdsToCheckExistingBuilds.distinct()
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
      try {
        return RepositoryManager.getLastCompatibleUpdateOfPlugin(IdeVersion.createIdeVersion(communityVersion), pluginId)
      } catch (e: Exception) {
        return null
      }
    }
    return null
  }


}

