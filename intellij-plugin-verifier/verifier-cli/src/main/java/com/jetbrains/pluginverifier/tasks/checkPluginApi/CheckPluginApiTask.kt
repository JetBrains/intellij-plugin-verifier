package com.jetbrains.pluginverifier.tasks.checkPluginApi

import com.jetbrains.pluginverifier.*
import com.jetbrains.pluginverifier.jdk.JdkDescriptorsCache
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.reporting.PluginVerificationReportage
import com.jetbrains.pluginverifier.resolution.PluginApiClassResolverProvider
import com.jetbrains.pluginverifier.tasks.Task
import com.jetbrains.pluginverifier.tasks.twoTargets.TwoTargetsVerificationResults
import com.jetbrains.pluginverifier.verifiers.filter.DynamicallyLoadedFilter

class CheckPluginApiTask(private val parameters: CheckPluginApiParams) : Task {

  override fun execute(
      reportage: PluginVerificationReportage,
      jdkDescriptorCache: JdkDescriptorsCache,
      pluginDetailsCache: PluginDetailsCache
  ): TwoTargetsVerificationResults {
    with(parameters) {
      val baseTarget = PluginVerificationTarget.Plugin(basePluginDetails.pluginInfo)
      val newTarget = PluginVerificationTarget.Plugin(newPluginDetails.pluginInfo)

      val baseClassResolverProvider = PluginApiClassResolverProvider(jdkDescriptorCache, jdkPath, basePluginDetails, basePluginPackageFilter)
      val newClassResolverProvider = PluginApiClassResolverProvider(jdkDescriptorCache, jdkPath, newPluginDetails, basePluginPackageFilter)

      val pluginsToCheck = pluginsSet.pluginsToCheck

      val verifiers = arrayListOf<PluginVerifier>()
      for (pluginInfo in pluginsToCheck) {
        verifiers.add(
            PluginVerifier(
                pluginInfo,
                baseTarget,
                problemsFilters,
                pluginDetailsCache,
                baseClassResolverProvider,
                listOf(DynamicallyLoadedFilter())
            )
        )

        verifiers.add(
            PluginVerifier(
                pluginInfo,
                newTarget,
                problemsFilters,
                pluginDetailsCache,
                newClassResolverProvider,
                listOf(DynamicallyLoadedFilter())
            )
        )
      }

      val results = runSeveralVerifiers(reportage, verifiers)
      return TwoTargetsVerificationResults(
          baseTarget,
          results.filter { it.verificationTarget == baseTarget },
          newTarget,
          results.filter { it.verificationTarget == newTarget }
      )
    }
  }

}
