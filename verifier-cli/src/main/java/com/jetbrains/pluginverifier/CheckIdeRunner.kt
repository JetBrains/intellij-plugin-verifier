package com.jetbrains.pluginverifier

import com.jetbrains.pluginverifier.tasks.CheckIdeParams
import com.jetbrains.pluginverifier.tasks.CheckIdeParamsBuilder
import com.jetbrains.pluginverifier.tasks.CheckIdeResult
import com.jetbrains.pluginverifier.tasks.CheckIdeTask

class CheckIdeRunner : TaskRunner<CheckIdeParams, CheckIdeParamsBuilder, CheckIdeResult, CheckIdeTask>() {
  override val commandName: String = "check-ide"

  override fun getParamsParser(): CheckIdeParamsBuilder = CheckIdeParamsBuilder()

  override fun getTask(parameters: CheckIdeParams): CheckIdeTask = CheckIdeTask(parameters)

}