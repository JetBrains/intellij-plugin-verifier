package com.jetbrains.pluginverifier

import com.jetbrains.pluginverifier.configurations.CheckIdeParams
import com.jetbrains.pluginverifier.configurations.CheckIdeParamsBuilder
import com.jetbrains.pluginverifier.configurations.CheckIdeResult
import com.jetbrains.pluginverifier.configurations.CheckIdeTask

class CheckIdeRunner : TaskRunner<CheckIdeParams, CheckIdeParamsBuilder, CheckIdeResult, CheckIdeTask>() {
  override val commandName: String = "check-ide"

  override fun getParamsParser(): CheckIdeParamsBuilder = CheckIdeParamsBuilder()

  override fun getTask(parameters: CheckIdeParams): CheckIdeTask = CheckIdeTask(parameters)

}