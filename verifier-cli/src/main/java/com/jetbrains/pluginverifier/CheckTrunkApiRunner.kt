package com.jetbrains.pluginverifier

import com.jetbrains.pluginverifier.configurations.CheckTrunkApiConfiguration
import com.jetbrains.pluginverifier.configurations.CheckTrunkApiParams
import com.jetbrains.pluginverifier.configurations.CheckTrunkApiParamsParser
import com.jetbrains.pluginverifier.configurations.CheckTrunkApiResults

class CheckTrunkApiRunner : ConfigurationRunner<CheckTrunkApiParams, CheckTrunkApiParamsParser, CheckTrunkApiResults, CheckTrunkApiConfiguration>() {
  override val commandName: String = "check-trunk-api"

  override fun getParamsParser(): CheckTrunkApiParamsParser = CheckTrunkApiParamsParser()

  override fun getConfiguration(parameters: CheckTrunkApiParams): CheckTrunkApiConfiguration = CheckTrunkApiConfiguration(parameters)

}