package com.jetbrains.pluginverifier.configurations

import com.intellij.structure.ide.Ide
import com.intellij.structure.plugin.PluginDependency
import com.jetbrains.pluginverifier.api.IdeDescriptor
import com.jetbrains.pluginverifier.api.PluginCoordinate
import com.jetbrains.pluginverifier.api.Result
import com.jetbrains.pluginverifier.api.VerifierParams
import com.jetbrains.pluginverifier.core.VerifierExecutor
import com.jetbrains.pluginverifier.dependencies.DefaultDependencyResolver
import com.jetbrains.pluginverifier.dependency.DependencyResolver
import com.jetbrains.pluginverifier.misc.closeLogged
import com.jetbrains.pluginverifier.plugin.CreatePluginResult
import com.jetbrains.pluginverifier.plugin.PluginCreator

class CheckPluginTask(parameters: CheckPluginParams) : Task<CheckPluginParams, CheckPluginResult>(parameters) {

  private var allPluginsToCheck: List<CreatePluginResult> = emptyList()

  private fun getDependencyResolver(ide: Ide): DependencyResolver = object : DependencyResolver {

    private val defaultDependencyResolver = DefaultDependencyResolver(ide)

    override fun resolve(dependency: PluginDependency, isModule: Boolean): DependencyResolver.Result {
      return findPluginInListOfPluginsToCheck(dependency) ?: defaultDependencyResolver.resolve(dependency, isModule)
    }

    private fun findPluginInListOfPluginsToCheck(dependency: PluginDependency): DependencyResolver.Result.FoundReady? {
      val createdPlugin = allPluginsToCheck
          .filterIsInstance<CreatePluginResult.OK>()
          .find { it.plugin.pluginId == dependency.id }
          ?: return null
      return DependencyResolver.Result.FoundReady(createdPlugin.plugin, createdPlugin.resolver)
    }
  }

  override fun execute(): CheckPluginResult {
    allPluginsToCheck = parameters.pluginCoordinates.map { PluginCreator.createPlugin(it) }
    try {
      return doExecute()
    } finally {
      allPluginsToCheck.forEach { it.closeLogged() }
    }
  }

  private fun doExecute(): CheckPluginResult {
    val results = arrayListOf<Result>()
    parameters.ideDescriptors.forEach { ideDescriptor ->
      val dependencyResolver = getDependencyResolver(ideDescriptor.ide)
      parameters.pluginCoordinates.mapTo(results) {
        doVerification(it, ideDescriptor, dependencyResolver)
      }
    }
    return CheckPluginResult(results)
  }

  private fun doVerification(pluginCoordinate: PluginCoordinate,
                             ideDescriptor: IdeDescriptor,
                             dependencyResolver: DependencyResolver): Result {
    val verifierParams = VerifierParams(parameters.jdkDescriptor, parameters.externalClassesPrefixes, parameters.problemsFilter, parameters.externalClasspath, dependencyResolver)
    val verifier = VerifierExecutor(verifierParams)
    verifier.use {
      val results = verifier.verify(listOf(pluginCoordinate to ideDescriptor), parameters.progress)
      return results.single()
    }
  }

}
