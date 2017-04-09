package com.jetbrains.pluginverifier.configurations

import com.jetbrains.pluginverifier.api.*


class CheckPluginConfiguration(val params: CheckPluginParams) : Configuration {

  override fun execute(): CheckPluginResults {
    val results = arrayListOf<Result>()
    for (ideDescriptor in params.ideDescriptors) {
      var extendedIdeDescriptor = ideDescriptor
      for (pluginDescriptor in params.pluginDescriptors) {
        val singlePluginCheck = listOf(pluginDescriptor to extendedIdeDescriptor)
        val vParams = VerifierParams(params.jdkDescriptor, singlePluginCheck, params.externalClassesPrefixes, params.problemsFilter, params.externalClasspath)
        val result: Result = Verifier(vParams).verify(params.progress).single()
        results.add(result)
        if (pluginDescriptor is PluginDescriptor.ByInstance) {
          val expandedIde = extendedIdeDescriptor.ide.getExpandedIde(pluginDescriptor.plugin)
          extendedIdeDescriptor = IdeDescriptor.ByInstance(expandedIde, extendedIdeDescriptor.ideResolver)
        }
      }
    }
    return CheckPluginResults(results)
  }


}
