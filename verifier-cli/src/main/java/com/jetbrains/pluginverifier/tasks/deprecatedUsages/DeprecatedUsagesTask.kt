package com.jetbrains.pluginverifier.tasks.deprecatedUsages

import com.jetbrains.plugin.structure.classes.resolvers.EmptyResolver
import com.jetbrains.pluginverifier.core.Verification
import com.jetbrains.pluginverifier.core.VerifierTask
import com.jetbrains.pluginverifier.misc.pluralizeWithNumber
import com.jetbrains.pluginverifier.parameters.VerifierParameters
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.reporting.verification.VerificationReportage
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.results.VerificationResult
import com.jetbrains.pluginverifier.tasks.Task
import com.jetbrains.pluginverifier.verifiers.IdeClassesVisitor

class DeprecatedUsagesTask(private val parameters: DeprecatedUsagesParams,
                           val pluginRepository: PluginRepository,
                           val pluginDetailsCache: PluginDetailsCache) : Task {

  override fun execute(verificationReportage: VerificationReportage) = with(parameters) {
    val verifierParams = VerifierParameters(
        emptyList(),
        emptyList(),
        EmptyResolver,
        true
    )
    val tasks = pluginsSet.pluginsToCheck.map { VerifierTask(it, parameters.jdkPath, ideDescriptor, dependencyFinder) }
    verificationReportage.logVerificationStage("Search of the deprecated API of ${ideDescriptor.ideVersion} in " + "plugin".pluralizeWithNumber(pluginsSet.pluginsToCheck.size) + " is about to start")
    val results = Verification.run(verifierParams, pluginDetailsCache, tasks, verificationReportage, jdkDescriptorsCache)
    val pluginToDeprecatedUsages = results.associateBy({ it.plugin }, { it.toDeprecatedUsages() })
    verificationReportage.logVerificationStage("Scan of all the deprecated API elements of ${ideDescriptor.ideVersion} is about to start")
    val deprecatedIdeApiElements = IdeClassesVisitor().detectIdeDeprecatedApiElements(ideDescriptor)
    verificationReportage.logVerificationStage("All stages are complete")
    DeprecatedUsagesResult(
        ideDescriptor.ideVersion,
        ideVersionForCompatiblePlugins,
        pluginToDeprecatedUsages,
        deprecatedIdeApiElements
    )
  }

  private fun VerificationResult.toDeprecatedUsages() = when (this) {
    is VerificationResult.OK -> deprecatedUsages
    is VerificationResult.StructureWarnings -> deprecatedUsages
    is VerificationResult.MissingDependencies -> deprecatedUsages
    is VerificationResult.CompatibilityProblems -> deprecatedUsages
    is VerificationResult.NotFound -> emptySet()
    is VerificationResult.FailedToDownload -> emptySet()
    is VerificationResult.InvalidPlugin -> emptySet()
  }


}

