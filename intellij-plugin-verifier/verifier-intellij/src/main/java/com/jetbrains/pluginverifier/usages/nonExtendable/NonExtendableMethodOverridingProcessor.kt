package com.jetbrains.pluginverifier.usages.nonExtendable

import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.method.MethodOverridingProcessor
import com.jetbrains.pluginverifier.verifiers.resolution.Method

class NonExtendableMethodOverridingProcessor(private val nonExtendableApiRegistrar: NonExtendableApiRegistrar) : MethodOverridingProcessor {
  override fun processMethodOverriding(method: Method, overriddenMethod: Method, context: VerificationContext) {
    if (overriddenMethod.isNonExtendable()) {
      nonExtendableApiRegistrar.registerNonExtendableApiUsage(NonExtendableMethodOverriding(overriddenMethod.location, method.location))
    }
  }
}