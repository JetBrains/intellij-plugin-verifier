package com.jetbrains.pluginverifier

import com.jetbrains.pluginverifier.tasks.CheckPluginParams
import com.jetbrains.pluginverifier.tasks.CheckPluginParamsBuilder
import com.jetbrains.pluginverifier.tasks.CheckPluginResult
import com.jetbrains.pluginverifier.tasks.CheckPluginTask

class CheckPluginRunner : TaskRunner<CheckPluginParams, CheckPluginParamsBuilder, CheckPluginResult, CheckPluginTask>() {
  override val commandName: String = "check-plugin"

  override fun getParamsParser(): CheckPluginParamsBuilder = CheckPluginParamsBuilder()

  override fun getTask(parameters: CheckPluginParams): CheckPluginTask = CheckPluginTask(parameters)

}