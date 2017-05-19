package com.jetbrains.pluginverifier.configurations

import com.intellij.structure.ide.IdeVersion
import com.jetbrains.pluginverifier.api.PluginDescriptor
import com.jetbrains.pluginverifier.api.Verifier
import com.jetbrains.pluginverifier.api.VerifierParams
import com.jetbrains.pluginverifier.format.UpdateInfo
import com.jetbrains.pluginverifier.plugin.CreatePluginResult
import com.jetbrains.pluginverifier.plugin.PluginCreator
import com.jetbrains.pluginverifier.repository.RepositoryManager
import com.jetbrains.pluginverifier.utils.VerificationResultToApiResultConverter


class CheckIdeConfiguration(val params: CheckIdeParams) : Configuration {

  override fun execute(): CheckIdeResults {
    val notExcludedPlugins = params.pluginsToCheck.filterNot { isExcluded(it) }
    return doExecute(notExcludedPlugins)
  }

  private fun isExcluded(pluginDescriptor: PluginDescriptor): Boolean {
    return when (pluginDescriptor) {
      is PluginDescriptor.ByUpdateInfo -> params.excludedPlugins.containsEntry(pluginDescriptor.updateInfo.pluginId, pluginDescriptor.updateInfo.version)
      is PluginDescriptor.ByInstance -> params.excludedPlugins.containsEntry(pluginDescriptor.createOk.success.plugin.pluginId, pluginDescriptor.createOk.success.plugin.pluginVersion)
      is PluginDescriptor.ByFileLock -> {
        PluginCreator.createPluginByFile(pluginDescriptor.fileLock.getFile()).use { createPluginResult ->
          if (createPluginResult is CreatePluginResult.OK) {
            val plugin = createPluginResult.success.plugin
            return params.excludedPlugins.containsEntry(plugin.pluginId, plugin.pluginVersion)
          }
          return true
        }
      }
    }
  }

  private fun doExecute(notExcludedPlugins: List<PluginDescriptor>): CheckIdeResults {
    val verifierParams = VerifierParams(params.jdkDescriptor, params.externalClassesPrefixes, params.problemsFilter, params.externalClassPath, params.dependencyResolver)
    val apiResultConverter = VerificationResultToApiResultConverter()
    val verifier = Verifier(verifierParams)
    verifier.use {
      notExcludedPlugins.forEach { verifier.verify(it, params.ideDescriptor) }
      val results = verifier.getVerificationResults(params.progress)
      return CheckIdeResults(params.ideDescriptor.ideVersion, apiResultConverter.convert(results), params.excludedPlugins, getMissingUpdatesProblems())
    }
  }

  private fun getMissingUpdatesProblems(): List<MissingCompatibleUpdate> {
    val ideVersion = params.ideDescriptor.ideVersion
    val existingUpdatesForIde = RepositoryManager
        .getLastCompatibleUpdates(ideVersion)
        .filterNot { params.excludedPlugins.containsEntry(it.pluginId, it.version) }
        .map { it.pluginId }
        .filterNotNull()
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

