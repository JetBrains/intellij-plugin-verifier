package com.jetbrains.pluginverifier.configurations

import com.intellij.structure.domain.IdeVersion
import com.jetbrains.pluginverifier.api.VManager
import com.jetbrains.pluginverifier.api.VParams
import com.jetbrains.pluginverifier.format.UpdateInfo
import com.jetbrains.pluginverifier.problems.NoCompatibleUpdatesProblem
import com.jetbrains.pluginverifier.repository.RepositoryManager


class CheckIdeConfiguration(val params: CheckIdeParams) : Configuration {
  override fun execute(): CheckIdeResults {
    val pluginsToCheck = params.pluginsToCheck.filterNot { params.excludedPlugins.containsEntry(it.pluginId, it.version) }.map { it to params.ideDescriptor }
    val vParams = VParams(params.jdkDescriptor, pluginsToCheck, params.vOptions, params.externalClassPath)
    val vResults = VManager.verify(vParams, params.progress)
    return CheckIdeResults(params.ideDescriptor.ideVersion, vResults, params.excludedPlugins, getMissingUpdatesProblems())
  }

  private fun getMissingUpdatesProblems(): List<NoCompatibleUpdatesProblem> {
    val ideVersion = params.ideDescriptor.ideVersion
    val existingUpdatesForIde = RepositoryManager.getInstance()
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
                "\nbut the Plugin repository does not offer to install it if you run the IDEA Ultimate."
            NoCompatibleUpdatesProblem(it, ideVersion.asString(), details)
          } else {
            NoCompatibleUpdatesProblem(it, ideVersion.asString(), "")
          }
        }
  }

  private fun getUpdateCompatibleWithCommunityEdition(pluginId: String, version: IdeVersion): UpdateInfo? {
    val ideVersion = version.asString()
    if (ideVersion.startsWith("IU-")) {
      val communityVersion = "IC-" + ideVersion.substringAfter(ideVersion, "IU-")
      try {
        return RepositoryManager.getInstance().getLastCompatibleUpdateOfPlugin(IdeVersion.createIdeVersion(communityVersion), pluginId)
      } catch (e: Exception) {
        return null
      }

    }
    return null
  }


}

