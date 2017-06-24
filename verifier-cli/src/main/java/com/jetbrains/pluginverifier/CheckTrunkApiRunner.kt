package com.jetbrains.pluginverifier

import com.jetbrains.pluginverifier.tasks.CheckTrunkApiParams
import com.jetbrains.pluginverifier.tasks.CheckTrunkApiParamsBuilder
import com.jetbrains.pluginverifier.tasks.CheckTrunkApiResult
import com.jetbrains.pluginverifier.tasks.CheckTrunkApiTask

class CheckTrunkApiRunner : TaskRunner<CheckTrunkApiParams, CheckTrunkApiParamsBuilder, CheckTrunkApiResult, CheckTrunkApiTask>() {
  override val commandName: String = "check-trunk-api"

  override fun getParamsParser(): CheckTrunkApiParamsBuilder = CheckTrunkApiParamsBuilder()

  override fun getTask(parameters: CheckTrunkApiParams): CheckTrunkApiTask = CheckTrunkApiTask(parameters)

}