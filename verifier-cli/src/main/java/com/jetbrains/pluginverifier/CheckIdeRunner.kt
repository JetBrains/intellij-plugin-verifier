package com.jetbrains.pluginverifier

import com.jetbrains.pluginverifier.tasks.*

class CheckIdeRunner : TaskRunner() {
  override val commandName: String = "check-ide"

  override fun getParametersBuilder(): TaskParametersBuilder = CheckIdeParamsBuilder()

  override fun createTask(parameters: TaskParameters): CheckIdeTask = CheckIdeTask(parameters as CheckIdeParams)

}