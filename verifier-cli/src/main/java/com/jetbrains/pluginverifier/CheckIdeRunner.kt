package com.jetbrains.pluginverifier

import com.jetbrains.pluginverifier.configurations.CheckIdeConfiguration
import com.jetbrains.pluginverifier.configurations.CheckIdeParams
import com.jetbrains.pluginverifier.configurations.CheckIdeParamsBuilder
import com.jetbrains.pluginverifier.configurations.CheckIdeResults

class CheckIdeRunner : ConfigurationRunner<CheckIdeParams, CheckIdeParamsBuilder, CheckIdeResults, CheckIdeConfiguration>() {
  override val commandName: String = "check-ide"

  override fun getParamsParser(): CheckIdeParamsBuilder = CheckIdeParamsBuilder()

  override fun getConfiguration(parameters: CheckIdeParams): CheckIdeConfiguration = CheckIdeConfiguration(parameters)

}