package com.jetbrains.pluginverifier.tasks.checkPluginApi

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.pluginverifier.PluginVerifier
import com.jetbrains.pluginverifier.VerificationTarget
import com.jetbrains.pluginverifier.VerifierExecutor
import com.jetbrains.pluginverifier.createPluginResolver
import com.jetbrains.pluginverifier.parameters.filtering.PluginIdAndVersion
import com.jetbrains.pluginverifier.parameters.filtering.ProblemsFilter
import com.jetbrains.pluginverifier.parameters.jdk.JdkDescriptorsCache
import com.jetbrains.pluginverifier.parameters.jdk.JdkPath
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.reporting.verification.VerificationReportage
import com.jetbrains.pluginverifier.repository.PluginInfo
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
      val baseTarget = VerificationTarget.Plugin(
          PluginIdAndVersion(basePluginDetails.plugin.pluginId!!, basePluginDetails.plugin.pluginVersion!!)
      )
      val basePluginVerifiers = buildVerifiers(
          pluginsSet.pluginsToCheck,
          basePluginDetails.pluginClassesLocations.createPluginResolver(),
          baseTarget,
          verificationReportage,
          pluginDetailsCache,
          jdkDescriptorCache,
          problemsFilters,
          jdkPath
      )

      val newTarget = VerificationTarget.Plugin(
          PluginIdAndVersion(newPluginDetails.plugin.pluginId!!, newPluginDetails.plugin.pluginVersion!!)
      )
      val newPluginVerifiers = buildVerifiers(
          pluginsSet.pluginsToCheck,
          newPluginDetails.pluginClassesLocations.createPluginResolver(),
          newTarget,
          verificationReportage,
          pluginDetailsCache,
          jdkDescriptorCache,
          problemsFilters,
          jdkPath
      )

      val results = verifierExecutor.verify(basePluginVerifiers + newPluginVerifiers)
      return NewProblemsResult.create(
          baseTarget,
          results.filter { it.verificationTarget == baseTarget },
          newTarget,
          results.filter { it.verificationTarget == newTarget }
      )
    }
  }

  private fun buildVerifiers(pluginsToCheck: List<PluginInfo>,
                             targetPluginResolver: Resolver,
                             verificationTarget: VerificationTarget,
                             verificationReportage: VerificationReportage,
                             pluginDetailsCache: PluginDetailsCache,
                             jdkDescriptorCache: JdkDescriptorsCache,
                             problemsFilters: List<ProblemsFilter>,
                             jdkPath: JdkPath): List<PluginVerifier> {
    val clsResolverProvider = PluginApiClsResolverProvider(jdkDescriptorCache, jdkPath, targetPluginResolver)
    return pluginsToCheck.map {
      PluginVerifier(
          it,
          verificationReportage,
          problemsFilters,
          false,
          pluginDetailsCache,
          clsResolverProvider,
          verificationTarget
      )
    }
  }

}
