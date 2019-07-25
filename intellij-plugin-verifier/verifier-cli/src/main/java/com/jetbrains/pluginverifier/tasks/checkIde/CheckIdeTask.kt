package com.jetbrains.pluginverifier.tasks.checkIde

import com.jetbrains.pluginverifier.PluginVerifier
import com.jetbrains.pluginverifier.VerificationTarget
import com.jetbrains.pluginverifier.VerifierExecutor
import com.jetbrains.pluginverifier.parameters.jdk.JdkDescriptorsCache
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.reporting.verification.Reportage
import com.jetbrains.pluginverifier.resolution.DefaultClassResolverProvider
import com.jetbrains.pluginverifier.tasks.Task
import com.jetbrains.pluginverifier.verifiers.filter.DynamicallyLoadedFilter

class CheckIdeTask(private val parameters: CheckIdeParams) : Task {

  override fun execute(
      reportage: Reportage,
      verifierExecutor: VerifierExecutor,
      jdkDescriptorCache: JdkDescriptorsCache,
      pluginDetailsCache: PluginDetailsCache
  ): CheckIdeResult {
    with(parameters) {
      val tasks = pluginsSet.pluginsToCheck
          .map {
            PluginVerifier(
                it,
                reportage,
                problemsFilters,
                pluginDetailsCache,
                DefaultClassResolverProvider(
                    dependencyFinder,
                    jdkDescriptorCache,
                    jdkPath,
                    ideDescriptor,
                    externalClassesPackageFilter
                ),
                VerificationTarget.Ide(ideDescriptor.ideVersion),
                ideDescriptor.brokenPlugins,
                listOf(DynamicallyLoadedFilter())
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

