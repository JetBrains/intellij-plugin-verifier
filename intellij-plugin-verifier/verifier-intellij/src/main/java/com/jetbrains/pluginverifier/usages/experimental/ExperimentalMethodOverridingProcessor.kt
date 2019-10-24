package com.jetbrains.pluginverifier.usages.experimental

import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.method.MethodOverridingProcessor
import com.jetbrains.pluginverifier.verifiers.resolution.Method

class ExperimentalMethodOverridingProcessor(private val experimentalApiRegistrar: ExperimentalApiRegistrar) : MethodOverridingProcessor {
  override fun processMethodOverriding(method: Method, overriddenMethod: Method, context: VerificationContext) {
    if (overriddenMethod.isExperimentalApi(context.classResolver)) {
      experimentalApiRegistrar.registerExperimentalApiUsage(
        ExperimentalMethodOverridden(
          overriddenMethod.location,
          method.location
        )
      )
    }
  }
}