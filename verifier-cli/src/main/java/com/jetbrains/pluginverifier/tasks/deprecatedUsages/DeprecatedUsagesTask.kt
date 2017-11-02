package com.jetbrains.pluginverifier.tasks.deprecatedUsages

import com.jetbrains.plugin.structure.classes.resolvers.EmptyResolver
import com.jetbrains.pluginverifier.core.Verification
import com.jetbrains.pluginverifier.parameters.VerifierParameters
import com.jetbrains.pluginverifier.plugin.PluginDetailsProvider
import com.jetbrains.pluginverifier.reporting.verification.VerificationReportage
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.results.Verdict
import com.jetbrains.pluginverifier.results.deprecated.DeprecatedApiUsage
import com.jetbrains.pluginverifier.tasks.Task
import com.jetbrains.pluginverifier.verifiers.IdeClassesVisitor

class DeprecatedUsagesTask(private val parameters: DeprecatedUsagesParams,
                           val pluginRepository: PluginRepository,
                           val pluginDetailsProvider: PluginDetailsProvider) : Task() {

  override fun execute(verificationReportage: VerificationReportage): DeprecatedUsagesResult {
    val verifierParams = VerifierParameters(
        externalClassesPrefixes = emptyList(),
        problemFilters = emptyList(),
        externalClassPath = EmptyResolver,
        dependencyFinder = parameters.dependencyFinder,
        findDeprecatedApiUsages = true
    )
    val tasks = parameters.pluginsToCheck.map { it to parameters.ideDescriptor }
    val results = Verification.run(verifierParams, pluginDetailsProvider, tasks, verificationReportage, parameters.jdkDescriptor)
    val pluginToDeprecatedUsages = results.associateBy({ it.plugin }, { it.verdict.toDeprecatedUsages() })
    val deprecatedIdeApiElements = IdeClassesVisitor().detectIdeDeprecatedApiElements(parameters.ideDescriptor)
    return DeprecatedUsagesResult(parameters.ideDescriptor.ideVersion, pluginToDeprecatedUsages, deprecatedIdeApiElements)
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

