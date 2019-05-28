package com.jetbrains.pluginverifier.usages

import com.jetbrains.pluginverifier.usages.experimental.ExperimentalApiUsage

interface ExperimentalApiRegistrar {
  fun registerExperimentalApiUsage(experimentalApiUsage: ExperimentalApiUsage)
}