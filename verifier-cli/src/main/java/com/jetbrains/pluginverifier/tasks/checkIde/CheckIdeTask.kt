package com.jetbrains.pluginverifier.tasks.checkIde

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.core.Verification
import com.jetbrains.pluginverifier.core.VerifierTask
import com.jetbrains.pluginverifier.misc.tryInvokeSeveralTimes
import com.jetbrains.pluginverifier.parameters.VerifierParameters
import com.jetbrains.pluginverifier.parameters.filtering.PluginIdAndVersion
import com.jetbrains.pluginverifier.parameters.filtering.toPluginIdAndVersion
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.reporting.verification.VerificationReportage
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.UpdateInfo
import com.jetbrains.pluginverifier.tasks.Task
import java.util.concurrent.TimeUnit

class CheckIdeTask(private val parameters: CheckIdeParams,
                   val pluginRepository: PluginRepository,
                   val pluginDetailsCache: PluginDetailsCache) : Task {

  //todo: get rid of excludedPlugins here?
  override fun execute(verificationReportage: VerificationReportage): CheckIdeResult {
    val notExcludedPlugins = parameters.pluginsToCheck.filterNot { it.toPluginIdAndVersion() in parameters.excludedPlugins }
    return doExecute(notExcludedPlugins, verificationReportage)
  }

  private fun doExecute(notExcludedPlugins: List<PluginInfo>, reportage: VerificationReportage): CheckIdeResult {
    val verifierParams = VerifierParameters(parameters.externalClassesPrefixes, parameters.problemsFilters, parameters.externalClassPath, false)
    val tasks = notExcludedPlugins.map { VerifierTask(it, parameters.ideDescriptor, parameters.dependencyFinder) }
    val results = Verification.run(verifierParams, pluginDetailsCache, tasks, reportage, parameters.jdkDescriptor)
    return CheckIdeResult(parameters.ideDescriptor.ideVersion, results, parameters.excludedPlugins, getMissingUpdatesProblems())
  }

  private fun getMissingUpdatesProblems(): List<MissingCompatibleUpdate> {
    val ideVersion = parameters.ideDescriptor.ideVersion
    val allCompatiblePlugins = pluginRepository.tryInvokeSeveralTimes(3, 5, TimeUnit.SECONDS, "fetch last compatible updates with $ideVersion") {
      getLastCompatiblePlugins(ideVersion)
    }
    val existingUpdatesForIde = allCompatiblePlugins
        .filterNot { PluginIdAndVersion(it.pluginId, it.version) in parameters.excludedPlugins }
        .map { it.pluginId }
        .toSet()

    return parameters.pluginIdsToCheckExistingBuilds.distinct()
        .filterNot { existingUpdatesForIde.contains(it) }
        .map {
          val buildForCommunity = getUpdateCompatibleWithCommunityEdition(it, ideVersion) as? UpdateInfo
          if (buildForCommunity != null) {
            val details = "\nNote: there is an update (#" + buildForCommunity.updateId + ") compatible with IDEA Community Edition, " +
                "but the Plugin repository does not offer to install it if you run the IDEA Ultimate."
            MissingCompatibleUpdate(it, ideVersion, details)
          } else {
            MissingCompatibleUpdate(it, ideVersion, "")
          }
        }
  }

  private fun getUpdateCompatibleWithCommunityEdition(pluginId: String, version: IdeVersion): PluginInfo? {
    val ideVersion = version.asString()
    if (ideVersion.startsWith("IU-")) {
      val communityVersion = "IC-" + ideVersion.substringAfter(ideVersion, "IU-")
      return try {
        pluginRepository.getLastCompatibleVersionOfPlugin(IdeVersion.createIdeVersion(communityVersion), pluginId)
      } catch (e: Exception) {
        null
      }
    }
    return null
  }


}

