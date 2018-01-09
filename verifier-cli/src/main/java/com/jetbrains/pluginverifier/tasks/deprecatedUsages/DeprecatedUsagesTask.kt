package com.jetbrains.pluginverifier.tasks.deprecatedUsages

import com.jetbrains.plugin.structure.classes.resolvers.EmptyResolver
import com.jetbrains.pluginverifier.core.Verification
import com.jetbrains.pluginverifier.core.VerifierTask
import com.jetbrains.pluginverifier.misc.pluralizeWithNumber
import com.jetbrains.pluginverifier.parameters.VerifierParameters
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.reporting.verification.VerificationReportage
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.results.Verdict
import com.jetbrains.pluginverifier.results.deprecated.DeprecatedApiUsage
import com.jetbrains.pluginverifier.tasks.Task
import com.jetbrains.pluginverifier.verifiers.IdeClassesVisitor

class DeprecatedUsagesTask(private val parameters: DeprecatedUsagesParams,
                           val pluginRepository: PluginRepository,
                           val pluginDetailsCache: PluginDetailsCache) : Task {

  override fun execute(verificationReportage: VerificationReportage): DeprecatedUsagesResult {
    with(parameters) {
      val verifierParams = VerifierParameters(
          externalClassesPrefixes = emptyList(),
          problemFilters = emptyList(),
          externalClassPath = EmptyResolver,
          findDeprecatedApiUsages = true
      )
      val tasks = pluginsToCheck.map { VerifierTask(it, ideDescriptor, dependencyFinder) }
      verificationReportage.logVerificationStage("Search of the deprecated API of ${ideDescriptor.ideVersion} in " + "plugin".pluralizeWithNumber(pluginsToCheck.size) + " is about to start")
      val results = Verification.run(verifierParams, pluginDetailsCache, tasks, verificationReportage, jdkDescriptor)
      val pluginToDeprecatedUsages = results.associateBy({ it.plugin }, { it.verdict.toDeprecatedUsages() })
      verificationReportage.logVerificationStage("Scan of all the deprecated API elements of ${ideDescriptor.ideVersion} is about to start")
      val deprecatedIdeApiElements = IdeClassesVisitor().detectIdeDeprecatedApiElements(ideDescriptor)
      verificationReportage.logVerificationStage("All stages are complete")
      return DeprecatedUsagesResult(ideDescriptor.ideVersion, ideVersionForCompatiblePlugins, pluginToDeprecatedUsages, deprecatedIdeApiElements)
    }
  }

  private fun Verdict.toDeprecatedUsages(): Set<DeprecatedApiUsage> = when (this) {
    is Verdict.OK -> deprecatedUsages
    is Verdict.Warnings -> deprecatedUsages
    is Verdict.MissingDependencies -> deprecatedUsages
    is Verdict.Problems -> deprecatedUsages
    is Verdict.NotFound -> emptySet()
    is Verdict.FailedToDownload -> emptySet()
    is Verdict.Bad -> emptySet()
  }


}

