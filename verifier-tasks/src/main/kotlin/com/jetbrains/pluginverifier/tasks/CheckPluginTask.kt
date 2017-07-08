package com.jetbrains.pluginverifier.tasks

import com.intellij.structure.ide.Ide
import com.intellij.structure.plugin.PluginDependency
import com.jetbrains.pluginverifier.api.*
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

  override fun execute(progress: Progress): CheckPluginResult {
    allPluginsToCheck = parameters.pluginCoordinates.map { PluginCreator.createPlugin(it) }
    try {
      return doExecute(progress)
    } finally {
      allPluginsToCheck.forEach { it.closeLogged() }
    }
  }

  private fun doExecute(progress: Progress): CheckPluginResult {
    val results = arrayListOf<Result>()
    parameters.ideDescriptors.forEach { ideDescriptor ->
      val dependencyResolver = getDependencyResolver(ideDescriptor.ide)
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
    val verifier = VerifierExecutor(verifierParams)
    verifier.use {
      val results = verifier.verify(listOf(pluginCoordinate to ideDescriptor), progress)
      return results.single()
    }
  }

}
