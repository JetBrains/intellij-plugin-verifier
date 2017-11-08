package com.jetbrains.pluginverifier.tasks.checkIde

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.core.Verification
import com.jetbrains.pluginverifier.misc.tryInvokeSeveralTimes
import com.jetbrains.pluginverifier.parameters.VerifierParameters
import com.jetbrains.pluginverifier.plugin.PluginCoordinate
import com.jetbrains.pluginverifier.plugin.PluginDetailsProvider
import com.jetbrains.pluginverifier.plugin.toPluginIdAndVersion
import com.jetbrains.pluginverifier.reporting.verification.VerificationReportage
import com.jetbrains.pluginverifier.repository.PluginIdAndVersion
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.UpdateInfo
import com.jetbrains.pluginverifier.tasks.Task
import java.util.concurrent.TimeUnit

class CheckIdeTask(private val parameters: CheckIdeParams,
                   val pluginRepository: PluginRepository,
                   val pluginDetailsProvider: PluginDetailsProvider) : Task() {

  //todo: get rid of excludedPlugins here?
  override fun execute(verificationReportage: VerificationReportage): CheckIdeResult {
    val notExcludedPlugins = parameters.pluginsToCheck.filterNot { it.toPluginIdAndVersion(pluginDetailsProvider) in parameters.excludedPlugins }
    return doExecute(notExcludedPlugins, verificationReportage)
  }

  private fun doExecute(notExcludedPlugins: List<PluginCoordinate>, reportage: VerificationReportage): CheckIdeResult {
    val verifierParams = VerifierParameters(parameters.externalClassesPrefixes, parameters.problemsFilters, parameters.externalClassPath, parameters.dependencyFinder, false)
    val tasks = notExcludedPlugins.map { it to parameters.ideDescriptor }
    val results = Verification.run(verifierParams, pluginDetailsProvider, tasks, reportage, parameters.jdkDescriptor)
    return CheckIdeResult(parameters.ideDescriptor.ideVersion, results, parameters.excludedPlugins, getMissingUpdatesProblems())
  }

  private fun getMissingUpdatesProblems(): List<MissingCompatibleUpdate> {
    val ideVersion = parameters.ideDescriptor.ideVersion
    val allCompatiblePlugins = pluginRepository.tryInvokeSeveralTimes(3, 5, TimeUnit.SECONDS, "fetch last compatible updates with $ideVersion") {
      getLastCompatibleUpdates(ideVersion)
    }
    val existingUpdatesForIde = allCompatiblePlugins
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

