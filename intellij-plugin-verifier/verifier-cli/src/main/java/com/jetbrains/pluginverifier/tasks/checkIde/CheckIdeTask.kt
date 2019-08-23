package com.jetbrains.pluginverifier.tasks.checkIde

import com.jetbrains.pluginverifier.*
import com.jetbrains.pluginverifier.jdk.JdkDescriptorsCache
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.reporting.PluginVerificationReportage
import com.jetbrains.pluginverifier.resolution.DefaultClassResolverProvider
import com.jetbrains.pluginverifier.tasks.Task
import com.jetbrains.pluginverifier.verifiers.filter.DynamicallyLoadedFilter

class CheckIdeTask(private val parameters: CheckIdeParams) : Task {

  override fun execute(
      reportage: PluginVerificationReportage,
      jdkDescriptorCache: JdkDescriptorsCache,
      pluginDetailsCache: PluginDetailsCache
  ): CheckIdeResult {
    with(parameters) {
      val ideTarget = PluginVerificationTarget.IDE(ideDescriptor.ide)
      val verifiers = pluginsSet.pluginsToCheck
          .map {
            PluginVerifier(
                it,
                ideTarget,
                problemsFilters,
                pluginDetailsCache,
                DefaultClassResolverProvider(
                    dependencyFinder,
                    jdkDescriptorCache,
                    jdkPath,
                    ideDescriptor,
                    externalClassesPackageFilter
                ),
                listOf(DynamicallyLoadedFilter())
            )
          }

      val results = runSeveralVerifiers(reportage, verifiers)

      return CheckIdeResult(
          ideTarget,
          results,
          missingCompatibleVersionsProblems
      )
    }
  }

}

