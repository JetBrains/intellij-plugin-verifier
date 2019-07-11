package com.jetbrains.pluginverifier.tasks.checkPluginApi

import com.jetbrains.pluginverifier.PluginVerifier
import com.jetbrains.pluginverifier.VerificationTarget
import com.jetbrains.pluginverifier.VerifierExecutor
import com.jetbrains.pluginverifier.createPluginResolver
import com.jetbrains.pluginverifier.parameters.jdk.JdkDescriptorsCache
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.reporting.verification.Reportage
import com.jetbrains.pluginverifier.resolution.PluginApiClassResolverProvider
import com.jetbrains.pluginverifier.tasks.Task
import com.jetbrains.pluginverifier.tasks.twoTargets.TwoTargetsVerificationResults
import com.jetbrains.pluginverifier.verifiers.filter.DynamicallyLoadedFilter

class CheckPluginApiTask(private val parameters: CheckPluginApiParams) : Task {

  override fun execute(
      reportage: Reportage,
      verifierExecutor: VerifierExecutor,
      jdkDescriptorCache: JdkDescriptorsCache,
      pluginDetailsCache: PluginDetailsCache
  ): TwoTargetsVerificationResults {
    with(parameters) {
      val baseTarget = VerificationTarget.Plugin(basePluginDetails.pluginInfo)
      val newTarget = VerificationTarget.Plugin(newPluginDetails.pluginInfo)

      val basePluginResolver = basePluginDetails.pluginClassesLocations.createPluginResolver()
      val newPluginResolver = newPluginDetails.pluginClassesLocations.createPluginResolver()

      val baseClassResolverProvider = PluginApiClassResolverProvider(jdkDescriptorCache, jdkPath, basePluginResolver, basePluginPackageFilter)
      val newClassResolverProvider = PluginApiClassResolverProvider(jdkDescriptorCache, jdkPath, newPluginResolver, basePluginPackageFilter)

      val pluginsToCheck = pluginsSet.pluginsToCheck

      val verifiers = arrayListOf<PluginVerifier>()
      for (pluginInfo in pluginsToCheck) {
        verifiers.add(
            PluginVerifier(
                pluginInfo,
                reportage,
                problemsFilters,
                false,
                pluginDetailsCache,
                baseClassResolverProvider,
                baseTarget,
                emptySet(),
                listOf(DynamicallyLoadedFilter())
            )
        )

        verifiers.add(
            PluginVerifier(
                pluginInfo,
                reportage,
                problemsFilters,
                false,
                pluginDetailsCache,
                newClassResolverProvider,
                newTarget,
                emptySet(),
                listOf(DynamicallyLoadedFilter())
            )
        )
      }

      val results = verifierExecutor.verify(verifiers)
      return TwoTargetsVerificationResults(
          baseTarget,
          results.filter { it.verificationTarget == baseTarget },
          newTarget,
          results.filter { it.verificationTarget == newTarget }
      )
    }
  }

}
