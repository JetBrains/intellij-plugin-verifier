package com.jetbrains.pluginverifier.verifiers

import com.jetbrains.pluginverifier.results.experimental.ExperimentalApiUsage

interface ExperimentalApiRegistrar {
  fun registerExperimentalApiUsage(experimentalApiUsage: ExperimentalApiUsage)

  object Empty : ExperimentalApiRegistrar {
    override fun registerExperimentalApiUsage(experimentalApiUsage: ExperimentalApiUsage) = Unit
  }
}