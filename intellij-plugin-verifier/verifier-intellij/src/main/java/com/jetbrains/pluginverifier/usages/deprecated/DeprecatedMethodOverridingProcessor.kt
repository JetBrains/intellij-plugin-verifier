package com.jetbrains.pluginverifier.usages.deprecated

import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.method.MethodOverridingProcessor
import com.jetbrains.pluginverifier.verifiers.resolution.Method

class DeprecatedMethodOverridingProcessor(private val deprecatedApiRegistrar: DeprecatedApiRegistrar) : MethodOverridingProcessor {
  override fun processMethodOverriding(method: Method, overriddenMethod: Method, context: VerificationContext) {
    val methodDeprecated = overriddenMethod.deprecationInfo
    if (methodDeprecated != null) {
      deprecatedApiRegistrar.registerDeprecatedUsage(
          DeprecatedMethodOverridden(
              overriddenMethod.location,
              method.location,
              methodDeprecated
          )
      )
    }
  }
}