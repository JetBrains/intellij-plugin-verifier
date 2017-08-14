package com.jetbrains.pluginverifier

import com.jetbrains.pluginverifier.tasks.*

class CheckPluginRunner : TaskRunner() {
  override val commandName: String = "check-plugin"

  override fun getParametersBuilder(): TaskParametersBuilder = CheckPluginParamsBuilder()

  override fun createTask(parameters: TaskParameters): CheckPluginTask = CheckPluginTask(parameters as CheckPluginParams)

}