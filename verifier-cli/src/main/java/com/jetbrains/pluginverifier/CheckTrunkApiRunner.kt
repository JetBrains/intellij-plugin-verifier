package com.jetbrains.pluginverifier

import com.jetbrains.pluginverifier.configurations.CheckTrunkApiParams
import com.jetbrains.pluginverifier.configurations.CheckTrunkApiParamsBuilder
import com.jetbrains.pluginverifier.configurations.CheckTrunkApiResult
import com.jetbrains.pluginverifier.configurations.CheckTrunkApiTask

class CheckTrunkApiRunner : TaskRunner<CheckTrunkApiParams, CheckTrunkApiParamsBuilder, CheckTrunkApiResult, CheckTrunkApiTask>() {
  override val commandName: String = "check-trunk-api"

  override fun getParamsParser(): CheckTrunkApiParamsBuilder = CheckTrunkApiParamsBuilder()

  override fun getTask(parameters: CheckTrunkApiParams): CheckTrunkApiTask = CheckTrunkApiTask(parameters)

}