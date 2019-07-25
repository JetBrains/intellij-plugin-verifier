package com.jetbrains.pluginverifier.tasks.deprecatedUsages

import com.jetbrains.plugin.structure.base.utils.pluralizeWithNumber
import com.jetbrains.pluginverifier.PluginVerifier
import com.jetbrains.pluginverifier.VerificationTarget
import com.jetbrains.pluginverifier.VerifierExecutor
import com.jetbrains.pluginverifier.parameters.jdk.JdkDescriptorsCache
import com.jetbrains.pluginverifier.verifiers.packages.DefaultPackageFilter
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.reporting.verification.Reportage
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.resolution.DefaultClassResolverProvider
import com.jetbrains.pluginverifier.results.VerificationResult
import com.jetbrains.pluginverifier.tasks.Task
import com.jetbrains.pluginverifier.usages.deprecated.DeprecatedApiUsage
import com.jetbrains.pluginverifier.verifiers.filter.DynamicallyLoadedFilter

class DeprecatedUsagesTask(private val parameters: DeprecatedUsagesParams, val pluginRepository: PluginRepository) : Task {

  override fun execute(
      reportage: Reportage,
      verifierExecutor: VerifierExecutor,
      jdkDescriptorCache: JdkDescriptorsCache,
      pluginDetailsCache: PluginDetailsCache
  ) = with(parameters) {
    val tasks = pluginsSet.pluginsToCheck.map {
      PluginVerifier(
          it,
          reportage,
          emptyList(),
          pluginDetailsCache,
          DefaultClassResolverProvider(
              dependencyFinder,
              jdkDescriptorCache,
              parameters.jdkPath,
              ideDescriptor,
              DefaultPackageFilter(emptyList())
          ),
          VerificationTarget.Ide(ideDescriptor.ideVersion),
          ideDescriptor.brokenPlugins,
          listOf(DynamicallyLoadedFilter())
      )
    }
    reportage.logVerificationStage("Search of the deprecated API of ${ideDescriptor.ideVersion} in " + "plugin".pluralizeWithNumber(pluginsSet.pluginsToCheck.size) + " is about to start")
    val results = verifierExecutor.verify(tasks)
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

  private fun VerificationResult.toDeprecatedUsages(): Set<DeprecatedApiUsage> = when (this) {
    is VerificationResult.OK -> deprecatedUsages
    is VerificationResult.CompatibilityWarnings -> deprecatedUsages
    is VerificationResult.MissingDependencies -> deprecatedUsages
    is VerificationResult.CompatibilityProblems -> deprecatedUsages
    is VerificationResult.NotFound -> emptySet()
    is VerificationResult.FailedToDownload -> emptySet()
    is VerificationResult.InvalidPlugin -> emptySet()
  }


}

