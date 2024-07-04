package com.jetbrains.pluginverifier.verifiers.filter

import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.hasAnnotation
import com.jetbrains.pluginverifier.verifiers.resolution.BinaryClassName
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileMember
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import com.jetbrains.pluginverifier.verifiers.resolution.searchParentOverrides
import org.objectweb.asm.tree.AbstractInsnNode

class SameModuleUsageFilter(private val annotation: BinaryClassName) : ApiUsageFilter {
  override fun allow(
    invokedMethod: Method,
    invocationInstruction: AbstractInsnNode,
    callerMethod: Method,
    context: VerificationContext
  ) =
    invokedMethod
      .findAnnotatedElement(annotation, context)
      ?.let { isSameOrigin(it, callerMethod) } ?: false

  private fun isSameOrigin(method: Method, anotherMethod: Method): Boolean =
    method.containingClassFile.classFileOrigin == anotherMethod.containingClassFile.classFileOrigin

  private fun Method.findAnnotatedElement(annotationFqn: String, verificationContext: VerificationContext): Method? {
    if (hasAnnotation(annotationFqn)) return this
    return searchParentOverrides(verificationContext.classResolver).firstOrNull { (it, _) ->
      it.hasAnnotation(annotationFqn)
    }?.method
  }

  private fun ClassFileMember.hasAnnotation(annotationName: BinaryClassName): Boolean =
    annotations.hasAnnotation(annotationName)
}