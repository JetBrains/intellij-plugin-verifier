package com.jetbrains.pluginverifier

import com.jetbrains.pluginverifier.configurations.Configuration
import com.jetbrains.pluginverifier.configurations.ConfigurationParams
import com.jetbrains.pluginverifier.configurations.ConfigurationParamsBuilder
import com.jetbrains.pluginverifier.configurations.ConfigurationResults

/**
 * @author Sergey Patrikeev
 */
abstract class ConfigurationRunner<Params : ConfigurationParams, out ParamsParser : ConfigurationParamsBuilder<Params>, out Result : ConfigurationResults, out Conf : Configuration<Params, Result>> {

  abstract val commandName: String

  abstract fun getParamsParser(): ParamsParser

  abstract fun getConfiguration(parameters: Params): Conf

}