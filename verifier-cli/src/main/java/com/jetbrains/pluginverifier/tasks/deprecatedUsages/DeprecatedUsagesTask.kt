package com.jetbrains.pluginverifier.tasks.deprecatedUsages

import com.jetbrains.plugin.structure.classes.resolvers.EmptyResolver
import com.jetbrains.pluginverifier.core.Verification
import com.jetbrains.pluginverifier.parameters.VerifierParameters
import com.jetbrains.pluginverifier.plugin.PluginDetailsProvider
import com.jetbrains.pluginverifier.reporting.verification.VerificationReportage
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.tasks.Task

class DeprecatedUsagesTask(private val parameters: DeprecatedUsagesParams,
                           val pluginRepository: PluginRepository,
                           val pluginDetailsProvider: PluginDetailsProvider) : Task() {

  override fun execute(verificationReportage: VerificationReportage): DeprecatedUsagesResult {
    val verifierParams = VerifierParameters(
        emptyList(),
        emptyList(),
        EmptyResolver,
        parameters.dependencyFinder,
        true
    )
    val tasks = parameters.pluginsToCheck.map { it to parameters.ideDescriptor }
    val results = Verification.run(verifierParams, pluginDetailsProvider, tasks, verificationReportage, parameters.jdkDescriptor)
    return DeprecatedUsagesResult(parameters.ideDescriptor.ideVersion, results)
  }

}

