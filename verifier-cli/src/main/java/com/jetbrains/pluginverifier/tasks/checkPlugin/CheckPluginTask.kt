package com.jetbrains.pluginverifier.tasks.checkPlugin

import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.pluginverifier.core.Verification
import com.jetbrains.pluginverifier.core.VerifierTask
import com.jetbrains.pluginverifier.dependencies.resolution.DependencyFinder
import com.jetbrains.pluginverifier.dependencies.resolution.IdeDependencyFinder
import com.jetbrains.pluginverifier.misc.closeLogged
import com.jetbrains.pluginverifier.parameters.VerifierParameters
import com.jetbrains.pluginverifier.plugin.PluginDetails
import com.jetbrains.pluginverifier.plugin.PluginDetailsProvider
import com.jetbrains.pluginverifier.reporting.verification.VerificationReportage
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.tasks.Task

class CheckPluginTask(private val parameters: CheckPluginParams,
                      private val pluginRepository: PluginRepository,
                      private val pluginDetailsProvider: PluginDetailsProvider) : Task() {

  private fun createDependencyFinder(ide: Ide, pluginDetails: List<PluginDetails>): DependencyFinder = object : DependencyFinder {

    private val ideDependencyResolver = IdeDependencyFinder(ide, pluginRepository, pluginDetailsProvider)

    override fun findPluginDependency(dependency: PluginDependency): DependencyFinder.Result =
        findPluginInListOfPluginsToCheck(dependency) ?: ideDependencyResolver.findPluginDependency(dependency)

    private fun findPluginInListOfPluginsToCheck(dependency: PluginDependency): DependencyFinder.Result? =
        pluginDetails.mapNotNull {
          val plugin = it.plugin
          if (plugin?.pluginId == dependency.id) {
            when (it) {
              is PluginDetails.ByFileLock -> DependencyFinder.Result.FoundOpenPluginAndClasses(it.plugin, it.warnings, it.pluginClassesLocations)
              is PluginDetails.FoundOpenPluginAndClasses -> DependencyFinder.Result.FoundOpenPluginAndClasses(it.plugin, it.warnings, it.pluginClassesLocations)
              is PluginDetails.FoundOpenPluginWithoutClasses -> DependencyFinder.Result.FoundOpenPluginWithoutClasses(it.plugin)
              is PluginDetails.BadPlugin -> null
              is PluginDetails.FailedToDownload -> null
              is PluginDetails.NotFound -> null
            }
          } else {
            null
          }
        }.firstOrNull()

  }

  override fun execute(verificationReportage: VerificationReportage): CheckPluginResult {
    val pluginCoordinates = parameters.pluginCoordinates
    val allPluginsToCheck = pluginCoordinates.map { pluginDetailsProvider.providePluginDetails(it) }
    try {
      return doExecute(verificationReportage, allPluginsToCheck)
    } finally {
      allPluginsToCheck.forEach { it.closeLogged() }
    }
  }

  private fun doExecute(verificationReportage: VerificationReportage, pluginDetails: List<PluginDetails>): CheckPluginResult {
    val tasks = parameters.ideDescriptors.flatMap { ideDescriptor ->
      val dependencyFinder = createDependencyFinder(ideDescriptor.ide, pluginDetails)
      parameters.pluginCoordinates.map { pluginCoordinate ->
        VerifierTask(pluginCoordinate, ideDescriptor, dependencyFinder)
      }
    }

    val verifierParams = VerifierParameters(
        parameters.externalClassesPrefixes,
        parameters.problemsFilters,
        parameters.externalClasspath,
        true
    )
    val results = Verification.run(verifierParams, pluginDetailsProvider, tasks, verificationReportage, parameters.jdkDescriptor)
    return CheckPluginResult(results)
  }

}
