package com.jetbrains.pluginverifier.tasks.checkPluginApi

import com.jetbrains.pluginverifier.PluginVerifier
import com.jetbrains.pluginverifier.VerificationTarget
import com.jetbrains.pluginverifier.VerifierExecutor
import com.jetbrains.pluginverifier.createPluginResolver
import com.jetbrains.pluginverifier.parameters.filtering.toPluginIdAndVersion
import com.jetbrains.pluginverifier.parameters.jdk.JdkDescriptorsCache
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.reporting.verification.VerificationReportage
import com.jetbrains.pluginverifier.tasks.Task
import com.jetbrains.pluginverifier.tasks.common.NewProblemsResult

class CheckPluginApiTask(private val parameters: CheckPluginApiParams) : Task {

  override fun execute(
      verificationReportage: VerificationReportage,
      verifierExecutor: VerifierExecutor,
      jdkDescriptorCache: JdkDescriptorsCache,
      pluginDetailsCache: PluginDetailsCache
  ): NewProblemsResult {
    with(parameters) {
      val baseTarget = VerificationTarget.Plugin(basePluginDetails.plugin.toPluginIdAndVersion())
      val newTarget = VerificationTarget.Plugin(newPluginDetails.plugin.toPluginIdAndVersion())

      val basePluginResolver = basePluginDetails.pluginClassesLocations.createPluginResolver()
      val newPluginResolver = newPluginDetails.pluginClassesLocations.createPluginResolver()

      val baseClsResolverProvider = PluginApiClsResolverProvider(jdkDescriptorCache, jdkPath, basePluginResolver, basePluginPackageFilter)
      val newClsResolverProvider = PluginApiClsResolverProvider(jdkDescriptorCache, jdkPath, newPluginResolver, basePluginPackageFilter)

      val pluginsToCheck = pluginsSet.pluginsToCheck

      val verifiers = arrayListOf<PluginVerifier>()
      for (pluginInfo in pluginsToCheck) {
        verifiers.add(
            PluginVerifier(
                pluginInfo,
                verificationReportage,
                problemsFilters,
                false,
                pluginDetailsCache,
                baseClsResolverProvider,
                baseTarget
            )
        )

        verifiers.add(
            PluginVerifier(
                pluginInfo,
                verificationReportage,
                problemsFilters,
                false,
                pluginDetailsCache,
                newClsResolverProvider,
                newTarget
            )
        )
      }

      val results = verifierExecutor.verify(verifiers)
      return NewProblemsResult.create(
          baseTarget,
          results.filter { it.verificationTarget == baseTarget },
          newTarget,
          results.filter { it.verificationTarget == newTarget }
      )
    }
  }

}
