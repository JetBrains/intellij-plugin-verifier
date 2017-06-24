package com.jetbrains.pluginverifier

import com.jetbrains.pluginverifier.configurations.CheckPluginConfiguration
import com.jetbrains.pluginverifier.configurations.CheckPluginParams
import com.jetbrains.pluginverifier.configurations.CheckPluginParamsParser
import com.jetbrains.pluginverifier.configurations.CheckPluginResults

class CheckPluginRunner : ConfigurationRunner<CheckPluginParams, CheckPluginParamsParser, CheckPluginResults, CheckPluginConfiguration>() {
  override val commandName: String = "check-plugin"

  override fun getParamsParser(): CheckPluginParamsParser = CheckPluginParamsParser()

  override fun getConfiguration(parameters: CheckPluginParams): CheckPluginConfiguration = CheckPluginConfiguration(parameters)

}