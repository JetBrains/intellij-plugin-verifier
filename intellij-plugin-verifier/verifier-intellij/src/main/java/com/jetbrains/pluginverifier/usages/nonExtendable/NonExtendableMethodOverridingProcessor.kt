package com.jetbrains.pluginverifier.usages.nonExtendable

import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.overriding.MethodOverridingProcessor
import com.jetbrains.pluginverifier.verifiers.resolution.Method

class NonExtendableMethodOverridingProcessor : MethodOverridingProcessor {
  override fun processMethodOverriding(method: Method, overriddenMethod: Method, context: VerificationContext) {
    if (context is NonExtendableApiRegistrar && overriddenMethod.isNonExtendable()) {
      context.registerNonExtendableApiUsage(NonExtendableMethodOverriding(overriddenMethod.location, method.location))
    }
  }
}