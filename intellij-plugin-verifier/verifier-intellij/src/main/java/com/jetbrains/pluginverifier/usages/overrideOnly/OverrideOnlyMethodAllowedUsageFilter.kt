package com.jetbrains.pluginverifier.usages.overrideOnly

import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.filter.ApiUsageFilter
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import com.jetbrains.pluginverifier.verifiers.resolution.isCallOfSuperMethod
import org.objectweb.asm.tree.AbstractInsnNode

class OverrideOnlyMethodAllowedUsageFilter(private val allowedMethodDescriptor: MethodDescriptor) : ApiUsageFilter {

  private val isDelegateCall = DelegateCallOnOverrideOnlyUsageFilter()

  override fun allowMethodInvocation(invokedMethod: Method,
                                     invocationInstruction: AbstractInsnNode,
                                     callerMethod: Method,
                                     context: VerificationContext): Boolean {

    val superCall = isSuperCall(callerMethod, invokedMethod, invocationInstruction)
    return superCall || isDelegateCall.allowMethodInvocation(invokedMethod, invocationInstruction, callerMethod, context)
  }

  private fun isSuperCall(callerMethod: Method, resolvedMethod: Method, instructionNode: AbstractInsnNode): Boolean {
    return (resolvedMethod.matches(allowedMethodDescriptor)
      && isCallOfSuperMethod(callerMethod, resolvedMethod, instructionNode))
  }

}

data class MethodDescriptor(private val methodName: String, private val descriptor: String)

fun Method.matches(methodDescriptor: MethodDescriptor): Boolean =
  MethodDescriptor(name, descriptor) == methodDescriptor

