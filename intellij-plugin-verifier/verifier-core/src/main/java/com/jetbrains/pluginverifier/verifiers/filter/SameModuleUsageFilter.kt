package com.jetbrains.pluginverifier.verifiers.filter

import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.hasAnnotation
import com.jetbrains.pluginverifier.verifiers.resolution.BinaryClassName
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileMember
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import com.jetbrains.pluginverifier.verifiers.resolution.searchParentOverrides
import org.objectweb.asm.tree.AbstractInsnNode

private const val DISABLE_SAME_MODULE_INVOCATIONS: BinaryClassName = "com/jetbrains/pluginverifier/verifiers/resolution/DisableSameModuleInvocations"

class SameModuleUsageFilter : ApiUsageFilter {
  override fun allowMethodInvocation(
    invokedMethod: Method,
    invocationInstruction: AbstractInsnNode,
    callerMethod: Method,
    context: VerificationContext
  ): Boolean {

    return isSameOrigin(invokedMethod, callerMethod) && !invokedMethod.isAnnotationPresent(DISABLE_SAME_MODULE_INVOCATIONS, context)
  }

  private fun isSameOrigin(method: Method, anotherMethod: Method): Boolean =
    method.containingClassFile.classFileOrigin == anotherMethod.containingClassFile.classFileOrigin

  private fun Method.isAnnotationPresent(annotationFqn: String, verificationContext: VerificationContext): Boolean {
    if (hasAnnotation(annotationFqn)) return true
    return searchParentOverrides(verificationContext.classResolver).any { (it, _) ->
      it.hasAnnotation(annotationFqn)
    }
  }

  private fun ClassFileMember.hasAnnotation(annotationName: BinaryClassName): Boolean =
    annotations.hasAnnotation(annotationName)
}