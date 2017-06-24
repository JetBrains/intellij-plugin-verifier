package com.jetbrains.pluginverifier

import com.jetbrains.pluginverifier.configurations.CheckPluginConfiguration
import com.jetbrains.pluginverifier.configurations.CheckPluginParams
import com.jetbrains.pluginverifier.configurations.CheckPluginParamsBuilder
import com.jetbrains.pluginverifier.configurations.CheckPluginResults

class CheckPluginRunner : ConfigurationRunner<CheckPluginParams, CheckPluginParamsBuilder, CheckPluginResults, CheckPluginConfiguration>() {
  override val commandName: String = "check-plugin"

  override fun getParamsParser(): CheckPluginParamsBuilder = CheckPluginParamsBuilder()

  override fun getConfiguration(parameters: CheckPluginParams): CheckPluginConfiguration = CheckPluginConfiguration(parameters)

}