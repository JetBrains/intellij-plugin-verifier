package com.jetbrains.pluginverifier.configurations

import com.jetbrains.pluginverifier.api.VManager
import com.jetbrains.pluginverifier.api.VParams


class CheckPluginConfiguration(val params: CheckPluginParams) : Configuration {

  override fun execute(): CheckPluginResults {
    val pluginsToCheck = params.pluginDescriptors.map { p -> params.ideDescriptors.map { p to it } }.flatten()
    val vParams = VParams(params.jdkDescriptor, pluginsToCheck, params.vOptions, params.externalClasspath, params.resolveDependenciesWithin)
    val vResults = VManager.verify(vParams, params.progress)

    return CheckPluginResults(vResults)
  }


}
