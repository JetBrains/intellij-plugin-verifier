package com.jetbrains.pluginverifier.configurations

import com.jetbrains.pluginverifier.api.*


class CheckPluginConfiguration(val params: CheckPluginParams) : Configuration {

  override fun execute(): CheckPluginResults {
    val results = arrayListOf<VResult>()
    params.ideDescriptors.forEach { ideDescriptor ->
      var extendedIdeDescriptor = ideDescriptor
      params.pluginDescriptors.forEach { pluginDescriptor ->
        val singleCheck = listOf(pluginDescriptor to extendedIdeDescriptor)
        val vParams = VParams(params.jdkDescriptor, singleCheck, params.vOptions, params.externalClasspath)
        val singleResult = VManager.verify(vParams, params.progress).results.single()
        results.add(singleResult)

        val checkedPlugin = singleResult.pluginDescriptor
        if (checkedPlugin is PluginDescriptor.ByInstance) {
          val expandedIde = extendedIdeDescriptor.ide.getExpandedIde(checkedPlugin.plugin)
          extendedIdeDescriptor = IdeDescriptor.ByInstance(expandedIde, extendedIdeDescriptor.ideResolver)
        }
      }
    }
    return CheckPluginResults(VResults(results))
  }


}
