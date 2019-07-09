package com.jetbrains.pluginverifier.usages.deprecated

import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.method.MethodOverridingProcessor
import com.jetbrains.pluginverifier.verifiers.resolution.Method

class DeprecatedMethodOverridingProcessor : MethodOverridingProcessor {
  override fun processMethodOverriding(method: Method, overriddenMethod: Method, context: VerificationContext) {
    val methodDeprecated = overriddenMethod.getDeprecationInfo()
    if (methodDeprecated != null && context is DeprecatedApiRegistrar) {
      context.registerDeprecatedUsage(
          DeprecatedMethodOverridden(
              overriddenMethod.location,
              method.location,
              methodDeprecated
          )
      )
    }
  }
}