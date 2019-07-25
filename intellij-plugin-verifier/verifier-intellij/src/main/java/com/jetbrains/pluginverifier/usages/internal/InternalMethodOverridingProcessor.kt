package com.jetbrains.pluginverifier.usages.internal

import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.method.MethodOverridingProcessor
import com.jetbrains.pluginverifier.verifiers.resolution.Method

class InternalMethodOverridingProcessor : MethodOverridingProcessor {
  override fun processMethodOverriding(method: Method, overriddenMethod: Method, context: VerificationContext) {
    if (context is InternalApiRegistrar && overriddenMethod.isInternalApi(context)) {
      context.registerInternalApiUsage(
          InternalMethodOverridden(
              overriddenMethod.location,
              method.location
          )
      )
    }
  }
}