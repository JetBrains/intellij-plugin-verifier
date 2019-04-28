package com.jetbrains.pluginverifier.verifiers.method

import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.resolution.Method

interface MethodVerifier {
  fun verify(method: Method, context: VerificationContext)
}
