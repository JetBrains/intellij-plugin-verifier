package com.jetbrains.pluginverifier.tasks.checkIde

import com.jetbrains.pluginverifier.PluginVerifier
import com.jetbrains.pluginverifier.VerificationTarget
import com.jetbrains.pluginverifier.VerifierExecutor
import com.jetbrains.pluginverifier.parameters.jdk.JdkDescriptorsCache
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.reporting.verification.VerificationReportage
import com.jetbrains.pluginverifier.tasks.Task
import com.jetbrains.pluginverifier.verifiers.resolution.DefaultClsResolverProvider

class CheckIdeTask(private val parameters: CheckIdeParams) : Task {

  override fun execute(
      verificationReportage: VerificationReportage,
      verifierExecutor: VerifierExecutor,
      jdkDescriptorCache: JdkDescriptorsCache,
      pluginDetailsCache: PluginDetailsCache
  ): CheckIdeResult {
    with(parameters) {
      val tasks = pluginsSet.pluginsToCheck
          .map {
            PluginVerifier(
                it,
                verificationReportage,
                problemsFilters,
                false,
                pluginDetailsCache,
                DefaultClsResolverProvider(
                    dependencyFinder,
                    jdkDescriptorCache,
                    jdkPath,
                    ideDescriptor,
                    externalClassesPrefixes
                ),
                VerificationTarget.Ide(ideDescriptor.ideVersion)
            )
          }

      val results = verifierExecutor.verify(tasks)

      return CheckIdeResult(
          ideDescriptor.ideVersion,
          results,
          missingCompatibleVersionsProblems
      )
    }
  }

}

