package com.jetbrains.pluginverifier.tasks.checkIde

import com.jetbrains.pluginverifier.core.Verification
import com.jetbrains.pluginverifier.core.VerifierTask
import com.jetbrains.pluginverifier.parameters.VerifierParameters
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.reporting.verification.VerificationReportage
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.tasks.Task

class CheckIdeTask(
    private val parameters: CheckIdeParams,
    val pluginRepository: PluginRepository,
    val pluginDetailsCache: PluginDetailsCache
) : Task {

  override fun execute(verificationReportage: VerificationReportage): CheckIdeResult {
    with(parameters) {
      val verifierParams = VerifierParameters(
          externalClassesPrefixes,
          problemsFilters,
          externalClassPath,
          false
      )
      val tasks = pluginsSet.pluginsToCheck
          .map { VerifierTask(it, jdkPath, ideDescriptor, dependencyFinder) }

      val results = Verification.run(
          verifierParams,
          pluginDetailsCache,
          tasks,
          verificationReportage,
          jdkDescriptorsCache
      )

      return CheckIdeResult(
          ideDescriptor.ideVersion,
          results,
          missingCompatibleVersionsProblems
      )
    }
  }

}

