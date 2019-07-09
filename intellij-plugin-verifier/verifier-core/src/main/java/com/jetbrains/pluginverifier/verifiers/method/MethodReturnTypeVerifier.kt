package com.jetbrains.pluginverifier.verifiers.method

import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.extractClassNameFromDescriptor
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import com.jetbrains.pluginverifier.verifiers.resolution.resolveClassChecked
import org.objectweb.asm.Type

class MethodReturnTypeVerifier : MethodVerifier {
  override fun verify(method: Method, context: VerificationContext) {
    val methodType = Type.getType(method.descriptor)
    val className = methodType.returnType.descriptor.extractClassNameFromDescriptor() ?: return
    context.classResolver.resolveClassChecked(className, method, context)
  }
}
