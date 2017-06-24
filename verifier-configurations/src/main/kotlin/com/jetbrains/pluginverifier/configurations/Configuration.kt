package com.jetbrains.pluginverifier.configurations

abstract class Configuration<Params : ConfigurationParams, out Results : ConfigurationResults>(protected val parameters: Params) {
  abstract fun execute(): Results
}
