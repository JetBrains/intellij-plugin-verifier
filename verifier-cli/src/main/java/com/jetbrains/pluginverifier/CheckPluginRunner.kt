package com.jetbrains.pluginverifier

import com.jetbrains.pluginverifier.configurations.CheckPluginParams
import com.jetbrains.pluginverifier.configurations.CheckPluginParamsBuilder
import com.jetbrains.pluginverifier.configurations.CheckPluginResult
import com.jetbrains.pluginverifier.configurations.CheckPluginTask

class CheckPluginRunner : TaskRunner<CheckPluginParams, CheckPluginParamsBuilder, CheckPluginResult, CheckPluginTask>() {
  override val commandName: String = "check-plugin"

  override fun getParamsParser(): CheckPluginParamsBuilder = CheckPluginParamsBuilder()

  override fun getTask(parameters: CheckPluginParams): CheckPluginTask = CheckPluginTask(parameters)

}