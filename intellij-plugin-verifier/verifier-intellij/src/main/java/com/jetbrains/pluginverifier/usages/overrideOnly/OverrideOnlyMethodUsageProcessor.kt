package com.jetbrains.pluginverifier.usages.overrideOnly

import com.jetbrains.pluginverifier.results.reference.MethodReference
import com.jetbrains.pluginverifier.usages.ApiUsageProcessor
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.findAnnotation
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import org.objectweb.asm.tree.AbstractInsnNode

class OverrideOnlyMethodUsageProcessor(private val overrideOnlyRegistrar: OverrideOnlyRegistrar) : ApiUsageProcessor {

  override fun processMethodInvocation(
    methodReference: MethodReference,
    resolvedMethod: Method,
    instructionNode: AbstractInsnNode,
    callerMethod: Method,
    context: VerificationContext
  ) {
    if (resolvedMethod.isOverrideOnlyMethod() && !isCallOfSuperConstructor(callerMethod, resolvedMethod)) {
      overrideOnlyRegistrar.registerOverrideOnlyMethodUsage(
        OverrideOnlyMethodUsage(methodReference, resolvedMethod.location, callerMethod.location)
      )
    }
  }

  private fun isCallOfSuperConstructor(callerMethod: Method, resolvedMethod: Method) =
    resolvedMethod.isConstructor
      && callerMethod.isConstructor
      && callerMethod.containingClassFile.superName == resolvedMethod.containingClassFile.name

  private fun Method.isOverrideOnlyMethod(): Boolean =
    runtimeInvisibleAnnotations.findAnnotation(overrideOnlyAnnotationName) != null
      || containingClassFile.runtimeInvisibleAnnotations.findAnnotation(overrideOnlyAnnotationName) != null

  private companion object {
    const val overrideOnlyAnnotationName = "org/jetbrains/annotations/ApiStatus\$OverrideOnly"
  }
}