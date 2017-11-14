package com.jetbrains.pluginverifier.tasks.checkPlugin

import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.pluginverifier.core.Verification
import com.jetbrains.pluginverifier.core.VerifierTask
import com.jetbrains.pluginverifier.dependencies.resolution.DependencyFinder
import com.jetbrains.pluginverifier.dependencies.resolution.IdeDependencyFinder
import com.jetbrains.pluginverifier.parameters.VerifierParameters
import com.jetbrains.pluginverifier.plugin.PluginCoordinate
import com.jetbrains.pluginverifier.plugin.PluginDetailsProvider
import com.jetbrains.pluginverifier.plugin.toPluginIdAndVersion
import com.jetbrains.pluginverifier.reporting.verification.VerificationReportage
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.tasks.Task

class CheckPluginTask(private val parameters: CheckPluginParams,
                      private val pluginRepository: PluginRepository,
                      private val pluginDetailsProvider: PluginDetailsProvider) : Task() {

  private fun createDependencyFinder(ide: Ide, localPlugins: LocalPlugins): DependencyFinder = object : DependencyFinder {

    private val ideDependencyResolver = IdeDependencyFinder(ide, pluginRepository, pluginDetailsProvider)

    override fun findPluginDependency(dependency: PluginDependency): DependencyFinder.Result =
        findPluginLocally(dependency) ?: ideDependencyResolver.findPluginDependency(dependency)

    private fun findPluginLocally(dependency: PluginDependency): DependencyFinder.Result? {
      val pluginCoordinate = localPlugins.findPluginCoordinate(dependency.id)
      return pluginCoordinate?.let { DependencyFinder.Result.FoundCoordinates(pluginCoordinate, pluginDetailsProvider) }
    }
  }

  private class LocalPlugins(private val pluginDetailsProvider: PluginDetailsProvider,
                             pluginCoordinates: List<PluginCoordinate>) {

    private val pluginIdAndVersionToCoordinates = pluginCoordinates
        .associateBy({ it.toPluginIdAndVersion(pluginDetailsProvider) }) { it }

    fun findPluginCoordinate(pluginId: String): PluginCoordinate? {
      val pluginIdAndVersion = pluginIdAndVersionToCoordinates.keys.find { it?.pluginId == pluginId }
      if (pluginIdAndVersion != null) {
        return pluginIdAndVersionToCoordinates[pluginIdAndVersion]!!
      }
      return null
    }
  }

  override fun execute(verificationReportage: VerificationReportage): CheckPluginResult {
    val localPlugins = LocalPlugins(pluginDetailsProvider, parameters.pluginCoordinates)
    return doExecute(verificationReportage, localPlugins)
  }

  private fun doExecute(verificationReportage: VerificationReportage, localPlugins: LocalPlugins): CheckPluginResult {
    val tasks = parameters.ideDescriptors.flatMap { ideDescriptor ->
      val dependencyFinder = createDependencyFinder(ideDescriptor.ide, localPlugins)
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
