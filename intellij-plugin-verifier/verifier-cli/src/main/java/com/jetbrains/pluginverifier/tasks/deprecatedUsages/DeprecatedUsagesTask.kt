package com.jetbrains.pluginverifier.tasks.deprecatedUsages

import com.jetbrains.plugin.structure.base.utils.pluralizeWithNumber
import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.PluginVerifier
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.reporting.PluginVerificationReportage
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.runSeveralVerifiers
import com.jetbrains.pluginverifier.tasks.Task
import com.jetbrains.pluginverifier.usages.deprecated.DeprecatedApiUsage
import com.jetbrains.pluginverifier.verifiers.filter.DynamicallyLoadedFilter

class DeprecatedUsagesTask(private val parameters: DeprecatedUsagesParams, val pluginRepository: PluginRepository) : Task {

  override fun execute(
      reportage: PluginVerificationReportage,
      pluginDetailsCache: PluginDetailsCache
  ) = with(parameters) {
    val verifiers = verificationDescriptors.map {
      PluginVerifier(
          it,
          emptyList(),
          pluginDetailsCache,
          listOf(DynamicallyLoadedFilter())
      )
    }
    reportage.logVerificationStage("Search of the deprecated API of ${ideDescriptor.ideVersion} in " + "plugin".pluralizeWithNumber(verificationDescriptors.size) + " is about to start")
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

