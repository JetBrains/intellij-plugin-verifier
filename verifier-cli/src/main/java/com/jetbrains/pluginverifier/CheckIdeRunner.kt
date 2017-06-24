package com.jetbrains.pluginverifier

import com.jetbrains.pluginverifier.configurations.CheckIdeConfiguration
import com.jetbrains.pluginverifier.configurations.CheckIdeParams
import com.jetbrains.pluginverifier.configurations.CheckIdeParamsParser
import com.jetbrains.pluginverifier.configurations.CheckIdeResults

class CheckIdeRunner : ConfigurationRunner<CheckIdeParams, CheckIdeParamsParser, CheckIdeResults, CheckIdeConfiguration>() {
  override val commandName: String = "check-ide"

  override fun getParamsParser(): CheckIdeParamsParser = CheckIdeParamsParser()

  override fun getConfiguration(parameters: CheckIdeParams): CheckIdeConfiguration = CheckIdeConfiguration(parameters)

}