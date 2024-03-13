package com.jetbrains.pluginverifier.usages.overrideOnly

import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.filter.ApiUsageFilter
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.MethodInsnNode

class OverrideOnlyMethodAllowedUsageFilter : ApiUsageFilter {

  private val isDelegateCall = DelegateCallOnOverrideOnlyUsageFilter()
  private val isSuperclassCall = SuperclassCallOnOverrideOnlyUsageFilter()

  override fun allowMethodInvocation(invokedMethod: Method,
                                     invocationInstruction: AbstractInsnNode,
                                     callerMethod: Method,
                                     context: VerificationContext): Boolean {

    val isSuperCall = isSuperclassCall.allowMethodInvocation(invokedMethod, invocationInstruction, callerMethod, context)
    val isDelegateCall = isDelegateCall.allowMethodInvocation(invokedMethod, invocationInstruction, callerMethod, context)
    return isSuperCall || isDelegateCall
  }
}

data class MethodDescriptor(private val methodName: String, private val descriptor: String)

fun Method.matches(method: Method): Boolean =
  MethodDescriptor(name, descriptor) == MethodDescriptor(method.name, method.descriptor)

fun MethodInsnNode.matches(method: Method): Boolean =
  MethodDescriptor(name, desc) == MethodDescriptor(method.name, method.descriptor)
