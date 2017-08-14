package com.jetbrains.pluginverifier

import com.jetbrains.pluginverifier.tasks.*

class CheckTrunkApiRunner : TaskRunner() {
  override val commandName: String = "check-trunk-api"

  override fun getParametersBuilder(): TaskParametersBuilder = CheckTrunkApiParamsBuilder()

  override fun createTask(parameters: TaskParameters): CheckTrunkApiTask = CheckTrunkApiTask(parameters as CheckTrunkApiParams)

}