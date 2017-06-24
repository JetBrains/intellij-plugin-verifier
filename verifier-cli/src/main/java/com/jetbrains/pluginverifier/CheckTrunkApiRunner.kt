package com.jetbrains.pluginverifier

import com.jetbrains.pluginverifier.configurations.CheckTrunkApiConfiguration
import com.jetbrains.pluginverifier.configurations.CheckTrunkApiParams
import com.jetbrains.pluginverifier.configurations.CheckTrunkApiParamsBuilder
import com.jetbrains.pluginverifier.configurations.CheckTrunkApiResults

class CheckTrunkApiRunner : ConfigurationRunner<CheckTrunkApiParams, CheckTrunkApiParamsBuilder, CheckTrunkApiResults, CheckTrunkApiConfiguration>() {
  override val commandName: String = "check-trunk-api"

  override fun getParamsParser(): CheckTrunkApiParamsBuilder = CheckTrunkApiParamsBuilder()

  override fun getConfiguration(parameters: CheckTrunkApiParams): CheckTrunkApiConfiguration = CheckTrunkApiConfiguration(parameters)

}