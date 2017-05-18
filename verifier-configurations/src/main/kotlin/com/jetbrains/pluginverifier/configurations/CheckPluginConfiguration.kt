package com.jetbrains.pluginverifier.configurations

import com.intellij.structure.ide.Ide
import com.intellij.structure.plugin.Plugin
import com.jetbrains.pluginverifier.api.*
import com.jetbrains.pluginverifier.dependency.DependencyResolver
import com.jetbrains.pluginverifier.misc.closeLogged
import com.jetbrains.pluginverifier.plugin.CreatePluginResult
import com.jetbrains.pluginverifier.plugin.PluginCreator
import com.jetbrains.pluginverifier.utils.DefaultDependencyResolver
import com.jetbrains.pluginverifier.utils.VerificationResultToApiResultConverter

class CheckPluginConfiguration(val params: CheckPluginParams) : Configuration {

  private var allPluginsToCheck: List<CreatePluginResult> = emptyList()

  private fun getDependencyResolver(ide: Ide): DependencyResolver = object : DependencyResolver {
    override fun resolve(dependencyId: String, isModule: Boolean, dependent: Plugin): DependencyResolver.Result {
      val foundPlugin = findPluginInListOfPluginsToCheck(dependencyId)
      if (foundPlugin != null) {
        return DependencyResolver.Result.Found(foundPlugin)
      }
      return DefaultDependencyResolver(ide).resolve(dependencyId, isModule, dependent)
    }

    private fun findPluginInListOfPluginsToCheck(dependencyId: String): CreatePluginResult.OK? = allPluginsToCheck
        .filterIsInstance<CreatePluginResult.OK>()
        .find { it.success.plugin.pluginId == dependencyId }
  }

  override fun execute(): CheckPluginResults {
    allPluginsToCheck = params.pluginDescriptors.map { PluginCreator.createPlugin(it) }
    try {
      return doExecute()
    } finally {
      allPluginsToCheck.forEach { it.closeLogged() }
    }
  }

  private fun doExecute(): CheckPluginResults {
    val results = arrayListOf<VerificationResult>()
    params.ideDescriptors.forEach { ideDescriptor ->
      val dependencyResolver = getDependencyResolver(ideDescriptor.createIdeResult.ide)
      params.pluginDescriptors.mapTo(results) {
        doVerification(it, ideDescriptor, dependencyResolver)
      }
    }
    return CheckPluginResults(VerificationResultToApiResultConverter().convert(results))
  }

  private fun doVerification(pluginDescriptor: PluginDescriptor,
                             ideDescriptor: IdeDescriptor,
                             dependencyResolver: DependencyResolver): VerificationResult {
    val singlePluginCheck = listOf(pluginDescriptor to ideDescriptor)
    val vParams = VerifierParams(params.jdkDescriptor, singlePluginCheck, params.externalClassesPrefixes, params.problemsFilter, params.externalClasspath, dependencyResolver)
    return Verifier(vParams).verify(params.progress).single()
  }

}
