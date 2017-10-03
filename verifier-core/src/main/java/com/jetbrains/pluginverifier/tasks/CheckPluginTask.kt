package com.jetbrains.pluginverifier.tasks

import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.pluginverifier.api.IdeDescriptor
import com.jetbrains.pluginverifier.api.Result
import com.jetbrains.pluginverifier.api.VerifierParams
import com.jetbrains.pluginverifier.core.Verification
import com.jetbrains.pluginverifier.dependencies.DependencyResolver
import com.jetbrains.pluginverifier.dependencies.IdeDependencyResolver
import com.jetbrains.pluginverifier.logging.VerificationLogger
import com.jetbrains.pluginverifier.misc.closeLogged
import com.jetbrains.pluginverifier.plugin.CreatePluginResult
import com.jetbrains.pluginverifier.plugin.PluginCoordinate
import com.jetbrains.pluginverifier.plugin.PluginCreator
import com.jetbrains.pluginverifier.plugin.create
import com.jetbrains.pluginverifier.repository.PluginRepository

class CheckPluginTask(private val parameters: CheckPluginParams,
                      private val pluginRepository: PluginRepository,
                      private val pluginCreator: PluginCreator) : Task() {

  private fun getDependencyResolver(ide: Ide, successfulCreations: List<CreatePluginResult.OK>): DependencyResolver = object : DependencyResolver {

    private val ideDependencyResolver = IdeDependencyResolver(ide, pluginRepository, pluginCreator)

    override fun resolve(dependency: PluginDependency): DependencyResolver.Result {
      return findPluginInListOfPluginsToCheck(dependency) ?: ideDependencyResolver.resolve(dependency)
    }

    private fun findPluginInListOfPluginsToCheck(dependency: PluginDependency): DependencyResolver.Result.FoundReady? {
      val createdPlugin = successfulCreations.find { it.plugin.pluginId == dependency.id } ?: return null
      return DependencyResolver.Result.FoundReady(createdPlugin.plugin, createdPlugin.pluginClassesLocations)
    }
  }

  override fun execute(logger: VerificationLogger): CheckPluginResult {
    val pluginCoordinates = parameters.pluginCoordinates
    val allPluginsToCheck = pluginCoordinates.map { it.create(pluginCreator) }
    try {
      val successfulCreations = allPluginsToCheck.filterIsInstance<CreatePluginResult.OK>()
      return doExecute(logger, successfulCreations)
    } finally {
      allPluginsToCheck.forEach { it.closeLogged() }
    }
  }

  private fun doExecute(progress: VerificationLogger, successfulCreations: List<CreatePluginResult.OK>): CheckPluginResult {
    val results = arrayListOf<Result>()
    parameters.ideDescriptors.forEach { ideDescriptor ->
      val dependencyResolver = getDependencyResolver(ideDescriptor.ide, successfulCreations)
      parameters.pluginCoordinates.mapTo(results) {
        doVerification(it, ideDescriptor, dependencyResolver, progress)
      }
    }
    return CheckPluginResult(results)
  }

  private fun doVerification(pluginCoordinate: PluginCoordinate,
                             ideDescriptor: IdeDescriptor,
                             dependencyResolver: DependencyResolver,
                             logger: VerificationLogger): Result {
    val verifierParams = VerifierParams(parameters.jdkDescriptor, parameters.externalClassesPrefixes, parameters.problemsFilters, parameters.externalClasspath, dependencyResolver)
    val tasks = listOf(pluginCoordinate to ideDescriptor)
    return Verification.run(verifierParams, pluginCreator, tasks, logger).single()
  }

}
