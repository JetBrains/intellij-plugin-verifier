package com.jetbrains.pluginverifier.usages.internal

import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.method.MethodOverridingProcessor
import com.jetbrains.pluginverifier.verifiers.resolution.Method

class InternalMethodOverridingProcessor(private val internalApiUsageRegistrar: InternalApiUsageRegistrar) : MethodOverridingProcessor {
  override fun processMethodOverriding(method: Method, overriddenMethod: Method, context: VerificationContext) {
    if (overriddenMethod.isInternalApi(context)) {
      internalApiUsageRegistrar.registerInternalApiUsage(
          InternalMethodOverridden(
              overriddenMethod.location,
              method.location
          )
      )
    }
  }
}