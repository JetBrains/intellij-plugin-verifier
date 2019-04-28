package com.jetbrains.pluginverifier.verifiers.method

import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.extractClassNameFromDescriptor
import com.jetbrains.pluginverifier.verifiers.resolution.Method

class MethodTryCatchVerifier : MethodVerifier {
  override fun verify(method: Method, context: VerificationContext) {
    for (block in method.tryCatchBlocks) {
      val catchException = block.type ?: continue
      val className = catchException.extractClassNameFromDescriptor() ?: continue
      context.classResolver.resolveClassChecked(className, method, context)
    }
  }
}
