package com.jetbrains.pluginverifier

import com.jetbrains.pluginverifier.configurations.Configuration
import com.jetbrains.pluginverifier.configurations.ConfigurationParams
import com.jetbrains.pluginverifier.configurations.ConfigurationParamsParser
import com.jetbrains.pluginverifier.configurations.ConfigurationResults

/**
 * @author Sergey Patrikeev
 */
abstract class ConfigurationRunner<P : ConfigurationParams, out PP : ConfigurationParamsParser<P>, R : ConfigurationResults, out C : Configuration<P, R>> {

  abstract val commandName: String

  abstract fun getParamsParser(): PP

  abstract fun getConfiguration(parameters: P): C

}