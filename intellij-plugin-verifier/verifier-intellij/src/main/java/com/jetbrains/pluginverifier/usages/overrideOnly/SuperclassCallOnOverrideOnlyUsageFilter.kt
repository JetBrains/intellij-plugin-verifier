package com.jetbrains.pluginverifier.usages.overrideOnly

import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.filter.ApiUsageFilter
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import com.jetbrains.pluginverifier.verifiers.resolution.isCallOfSuperMethod
import com.jetbrains.pluginverifier.verifiers.resolution.matches
import org.objectweb.asm.tree.AbstractInsnNode

class SuperclassCallOnOverrideOnlyUsageFilter  : ApiUsageFilter {
  override fun allow(invokedMethod: Method, invocationInstruction: AbstractInsnNode, callerMethod: Method, context: VerificationContext): Boolean {
    return callerMethod.matches(invokedMethod)
      && isCallOfSuperMethod(callerMethod, invokedMethod, invocationInstruction)
  }
}