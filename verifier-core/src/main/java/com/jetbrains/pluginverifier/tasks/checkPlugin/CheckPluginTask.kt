package com.jetbrains.pluginverifier.tasks.checkPlugin

import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.pluginverifier.core.Verification
import com.jetbrains.pluginverifier.dependencies.resolution.DependencyFinder
import com.jetbrains.pluginverifier.dependencies.resolution.IdeDependencyFinder
import com.jetbrains.pluginverifier.logging.VerificationLogger
import com.jetbrains.pluginverifier.misc.closeLogged
import com.jetbrains.pluginverifier.parameters.VerifierParameters
import com.jetbrains.pluginverifier.parameters.ide.IdeDescriptor
import com.jetbrains.pluginverifier.plugin.PluginCoordinate
import com.jetbrains.pluginverifier.plugin.PluginDetails
import com.jetbrains.pluginverifier.plugin.PluginDetailsProvider
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.results.Result
import com.jetbrains.pluginverifier.tasks.Task

class CheckPluginTask(private val parameters: CheckPluginParams,
                      private val pluginRepository: PluginRepository,
                      private val pluginDetailsProvider: PluginDetailsProvider) : Task() {

  private fun getDependencyResolver(ide: Ide, pluginDetails: List<PluginDetails>): DependencyFinder = object : DependencyFinder {

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

  override fun execute(logger: VerificationLogger): CheckPluginResult {
    val pluginCoordinates = parameters.pluginCoordinates
    val allPluginsToCheck = pluginCoordinates.map { pluginDetailsProvider.fetchPluginDetails(it) }
    try {
      return doExecute(logger, allPluginsToCheck)
    } finally {
      allPluginsToCheck.forEach { it.closeLogged() }
    }
  }

  private fun doExecute(progress: VerificationLogger, pluginDetails: List<PluginDetails>): CheckPluginResult {
    val results = arrayListOf<Result>()
    parameters.ideDescriptors.forEach { ideDescriptor ->
      val dependencyResolver = getDependencyResolver(ideDescriptor.ide, pluginDetails)
      parameters.pluginCoordinates.mapTo(results) {
        doVerification(it, ideDescriptor, dependencyResolver, progress)
      }
    }
    return CheckPluginResult(results)
  }

  private fun doVerification(pluginCoordinate: PluginCoordinate,
                             ideDescriptor: IdeDescriptor,
                             dependencyFinder: DependencyFinder,
                             logger: VerificationLogger): Result {
    val verifierParams = VerifierParameters(
        parameters.jdkDescriptor,
        parameters.externalClassesPrefixes,
        parameters.problemsFilters,
        parameters.externalClasspath,
        dependencyFinder
    )
    val tasks = listOf(pluginCoordinate to ideDescriptor)
    return Verification.run(verifierParams, pluginDetailsProvider, tasks, logger).single()
  }

}
