package com.jetbrains.pluginverifier.configurations

interface Configuration<in Params : ConfigurationParams, out Results : ConfigurationResults> {
  fun execute(parameters: Params): Results
}
