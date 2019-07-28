package com.jetbrains.pluginverifier.tasks.deprecatedUsages

import com.jetbrains.plugin.structure.base.utils.pluralizeWithNumber
import com.jetbrains.pluginverifier.*
import com.jetbrains.pluginverifier.jdk.JdkDescriptorsCache
import com.jetbrains.pluginverifier.verifiers.packages.DefaultPackageFilter
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.reporting.PluginVerificationReportage
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.resolution.DefaultClassResolverProvider
import com.jetbrains.pluginverifier.tasks.Task
import com.jetbrains.pluginverifier.usages.deprecated.DeprecatedApiUsage
import com.jetbrains.pluginverifier.verifiers.filter.DynamicallyLoadedFilter

class DeprecatedUsagesTask(private val parameters: DeprecatedUsagesParams, val pluginRepository: PluginRepository) : Task {

  override fun execute(
      reportage: PluginVerificationReportage,
      jdkDescriptorCache: JdkDescriptorsCache,
      pluginDetailsCache: PluginDetailsCache
  ) = with(parameters) {
    val verifiers = pluginsSet.pluginsToCheck.map {
      PluginVerifier(
          it,
          PluginVerificationTarget.IDE(ideDescriptor.ide),
          emptyList(),
          pluginDetailsCache,
          DefaultClassResolverProvider(
              dependencyFinder,
              jdkDescriptorCache,
              parameters.jdkPath,
              ideDescriptor,
              DefaultPackageFilter(emptyList())
          ),
          listOf(DynamicallyLoadedFilter())
      )
    }
    reportage.logVerificationStage("Search of the deprecated API of ${ideDescriptor.ideVersion} in " + "plugin".pluralizeWithNumber(pluginsSet.pluginsToCheck.size) + " is about to start")
    val results = runSeveralVerifiers(reportage, verifiers)
    val pluginToDeprecatedUsages = results.associateBy({ it.plugin }, { it.toDeprecatedUsages() })
    reportage.logVerificationStage("Scan of all the deprecated API elements of ${ideDescriptor.ideVersion} is about to start")
    val deprecatedIdeApiElements = DeprecatedIdeClassesVisitor().detectIdeDeprecatedApiElements(ideDescriptor)
    reportage.logVerificationStage("All stages are complete")
    DeprecatedUsagesResult(
        ideDescriptor.ideVersion,
        ideVersionForCompatiblePlugins,
        pluginToDeprecatedUsages,
        deprecatedIdeApiElements
    )
  }

  private fun PluginVerificationResult.toDeprecatedUsages(): Set<DeprecatedApiUsage> =
      if (this is PluginVerificationResult.Verified) {
        deprecatedUsages
      } else {
        emptySet()
      }
}

