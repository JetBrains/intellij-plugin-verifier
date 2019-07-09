package com.jetbrains.pluginverifier.verifiers.method

import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.resolution.Method

interface MethodOverridingProcessor {
  fun processMethodOverriding(method: Method, overriddenMethod: Method, context: VerificationContext)
}