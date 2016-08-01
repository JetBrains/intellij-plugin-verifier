package com.jetbrains.pluginverifier.configurations

import com.intellij.structure.domain.IdeVersion
import com.jetbrains.pluginverifier.api.VParams


class CheckIdeConfiguration(val params: CheckIdeParams) : Configuration {
  override fun execute(): CheckIdeResults {
    val pluginsToCheck = params.pluginsToCheck.filterNot { params.excludedPlugins.containsEntry(it.pluginId, it.version) }.map { it to params.ideDescriptor }
    val vParams = VParams(params.jdkDescriptor, pluginsToCheck, params.vOptions, params.externalClassPath)
    val vResults = com.jetbrains.pluginverifier.api.VManager.verify(vParams, params.progress)
    return CheckIdeResults(params.ideDescriptor.ideVersion, vResults, params.excludedPlugins, getMissingUpdatesProblems())
  }

  private fun getMissingUpdatesProblems(): List<com.jetbrains.pluginverifier.problems.NoCompatibleUpdatesProblem> {
    val ideVersion = params.ideDescriptor.ideVersion
    val existingUpdatesForIde = com.jetbrains.pluginverifier.repository.RepositoryManager.getInstance()
        .getLastCompatibleUpdates(ideVersion)
        .filterNot { params.excludedPlugins.containsEntry(it.pluginId, it.version) }
        .map { it.pluginId }
        .filterNotNull()
        .toSet()

    return params.pluginsToCheck.map { it.pluginId }.distinct()
        .filterNot { existingUpdatesForIde.contains(it) }
        .map {
          val buildForCommunity = getUpdateCompatibleWithCommunityEdition(it, ideVersion)
          if (buildForCommunity != null) {
            val details = "\nNote: there is an update (#" + buildForCommunity.updateId + ") compatible with IDEA Community Edition, " +
                "\nbut the Plugin repository does not offer to install it if you run the IDEA Ultimate."
            com.jetbrains.pluginverifier.problems.NoCompatibleUpdatesProblem(it, ideVersion.asString(), details)
          } else {
            com.jetbrains.pluginverifier.problems.NoCompatibleUpdatesProblem(it, ideVersion.asString(), "")
          }
        }
  }

  private fun getUpdateCompatibleWithCommunityEdition(pluginId: String, version: IdeVersion): com.jetbrains.pluginverifier.format.UpdateInfo? {
    val ideVersion = version.asString()
    if (ideVersion.startsWith("IU-")) {
      val communityVersion = "IC-" + ideVersion.substringAfter(ideVersion, "IU-")
      try {
        return com.jetbrains.pluginverifier.repository.RepositoryManager.getInstance().getLastCompatibleUpdateOfPlugin(IdeVersion.createIdeVersion(communityVersion), pluginId)
      } catch (e: Exception) {
        return null
      }

    }
    return null
  }


}

