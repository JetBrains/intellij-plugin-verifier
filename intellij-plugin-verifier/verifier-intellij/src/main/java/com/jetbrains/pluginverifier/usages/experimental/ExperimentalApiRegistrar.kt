package com.jetbrains.pluginverifier.usages.experimental

interface ExperimentalApiRegistrar {
  fun registerExperimentalApiUsage(experimentalApiUsage: ExperimentalApiUsage)
}