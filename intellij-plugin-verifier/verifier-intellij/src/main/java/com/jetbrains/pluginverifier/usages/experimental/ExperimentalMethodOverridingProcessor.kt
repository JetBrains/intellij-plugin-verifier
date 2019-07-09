package com.jetbrains.pluginverifier.usages.experimental

import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.method.MethodOverridingProcessor
import com.jetbrains.pluginverifier.verifiers.resolution.Method

class ExperimentalMethodOverridingProcessor : MethodOverridingProcessor {
  override fun processMethodOverriding(method: Method, overriddenMethod: Method, context: VerificationContext) {
    if (context is ExperimentalApiRegistrar && overriddenMethod.isExperimentalApi()) {
      context.registerExperimentalApiUsage(
          ExperimentalMethodOverridden(
              overriddenMethod.location,
              method.location
          )
      )
    }
  }
}