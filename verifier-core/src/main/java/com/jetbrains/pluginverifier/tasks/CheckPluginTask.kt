package com.jetbrains.pluginverifier.tasks

import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.pluginverifier.api.*
import com.jetbrains.pluginverifier.core.VerifierExecutor
import com.jetbrains.pluginverifier.dependencies.DependencyResolver
import com.jetbrains.pluginverifier.dependencies.IdeDependencyResolver
import com.jetbrains.pluginverifier.misc.closeLogged
import com.jetbrains.pluginverifier.plugin.CreatePluginResult
import com.jetbrains.pluginverifier.plugin.PluginCreator
import com.jetbrains.pluginverifier.repository.PluginRepository

class CheckPluginTask(private val parameters: CheckPluginParams,
                      private val pluginRepository: PluginRepository,
                      private val pluginCreator: PluginCreator) : Task() {

  private fun getDependencyResolver(ide: Ide, allPluginsToCheck: List<CreatePluginResult>): DependencyResolver = object : DependencyResolver {

    private val ideDependencyResolver = IdeDependencyResolver(ide, pluginRepository, pluginCreator)

    override fun resolve(dependency: PluginDependency): DependencyResolver.Result {
      return findPluginInListOfPluginsToCheck(dependency) ?: ideDependencyResolver.resolve(dependency)
    }

    private fun findPluginInListOfPluginsToCheck(dependency: PluginDependency): DependencyResolver.Result.FoundReady? {
      val createdPlugin = allPluginsToCheck
          .filterIsInstance<CreatePluginResult.OK>()
          .find { it.plugin.pluginId == dependency.id }
          ?: return null
      return DependencyResolver.Result.FoundReady(createdPlugin.plugin, createdPlugin.pluginClassesLocations)
    }
  }

  override fun execute(progress: Progress): CheckPluginResult {
    val allPluginsToCheck = parameters.pluginCoordinates.map { pluginCreator.createPlugin(it) }
    try {
      return doExecute(progress, allPluginsToCheck)
    } finally {
      allPluginsToCheck.forEach { it.closeLogged() }
    }
  }

  private fun doExecute(progress: Progress, allPluginsToCheck: List<CreatePluginResult>): CheckPluginResult {
    val results = arrayListOf<Result>()
    parameters.ideDescriptors.forEach { ideDescriptor ->
      val dependencyResolver = getDependencyResolver(ideDescriptor.ide, allPluginsToCheck)
      parameters.pluginCoordinates.mapTo(results) {
        doVerification(it, ideDescriptor, dependencyResolver, progress)
      }
    }
    return CheckPluginResult(results)
  }

  private fun doVerification(pluginCoordinate: PluginCoordinate,
                             ideDescriptor: IdeDescriptor,
                             dependencyResolver: DependencyResolver,
                             progress: Progress): Result {
    val verifierParams = VerifierParams(parameters.jdkDescriptor, parameters.externalClassesPrefixes, parameters.problemsFilter, parameters.externalClasspath, dependencyResolver)
    val verifier = VerifierExecutor(verifierParams, pluginCreator)
    verifier.use {
      val results = verifier.verify(listOf(pluginCoordinate to ideDescriptor), progress)
      return results.single()
    }
  }

}
