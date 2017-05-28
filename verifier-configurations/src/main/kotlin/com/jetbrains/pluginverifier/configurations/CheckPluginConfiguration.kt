package com.jetbrains.pluginverifier.configurations

import com.intellij.structure.ide.Ide
import com.intellij.structure.plugin.Plugin
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

class CheckPluginConfiguration : Configuration<CheckPluginParams, CheckPluginResults> {

  private var allPluginsToCheck: List<CreatePluginResult> = emptyList()

  private lateinit var params: CheckPluginParams

  private fun getDependencyResolver(ide: Ide): DependencyResolver = object : DependencyResolver {
    override fun resolve(dependencyId: String, isModule: Boolean, dependent: Plugin): DependencyResolver.Result {
      val foundPlugin = findPluginInListOfPluginsToCheck(dependencyId)
      if (foundPlugin != null) {
        return DependencyResolver.Result.FoundLocally(foundPlugin)
      }
      return DefaultDependencyResolver(ide).resolve(dependencyId, isModule, dependent)
    }

    private fun findPluginInListOfPluginsToCheck(dependencyId: String): CreatePluginResult.OK? {
      val inListOfPluginsToCheck = allPluginsToCheck
          .filterIsInstance<CreatePluginResult.OK>()
          .find { it.plugin.pluginId == dependencyId }
          ?: return null
      return PluginCreator.getNonCloseableOkResult(inListOfPluginsToCheck)
    }
  }

  override fun execute(parameters: CheckPluginParams): CheckPluginResults {
    params = parameters
    allPluginsToCheck = parameters.pluginCoordinates.map { PluginCreator.createPlugin(it) }
    try {
      return doExecute()
    } finally {
      allPluginsToCheck.forEach { it.closeLogged() }
    }
  }

  private fun doExecute(): CheckPluginResults {
    val results = arrayListOf<Result>()
    params.ideDescriptors.forEach { ideDescriptor ->
      val dependencyResolver = getDependencyResolver(ideDescriptor.ide)
      params.pluginCoordinates.mapTo(results) {
        doVerification(it, ideDescriptor, dependencyResolver)
      }
    }
    return CheckPluginResults(results)
  }

  private fun doVerification(pluginCoordinate: PluginCoordinate,
                             ideDescriptor: IdeDescriptor,
                             dependencyResolver: DependencyResolver): Result {
    val verifierParams = VerifierParams(params.jdkDescriptor, params.externalClassesPrefixes, params.problemsFilter, params.externalClasspath, dependencyResolver)
    val verifier = VerifierExecutor(verifierParams)
    verifier.use {
      val results = verifier.verify(listOf(pluginCoordinate to ideDescriptor), params.progress)
      return results.single()
    }
  }

}
