package com.jetbrains.pluginverifier.verifiers.method

import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.extractClassNameFromDescriptor
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import com.jetbrains.pluginverifier.verifiers.resolution.resolveClassChecked

class MethodLocalVarsVerifier : MethodVerifier {
  override fun verify(method: Method, context: VerificationContext) {
    for (variable in method.localVariables) {
      val className = variable.desc.extractClassNameFromDescriptor() ?: continue
      context.classResolver.resolveClassChecked(className, method, context)
    }
  }
}
