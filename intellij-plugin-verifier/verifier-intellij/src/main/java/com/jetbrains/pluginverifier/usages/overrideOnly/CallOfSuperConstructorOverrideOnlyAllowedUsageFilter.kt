package com.jetbrains.pluginverifier.usages.overrideOnly

import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.filter.ApiUsageFilter
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import org.objectweb.asm.tree.AbstractInsnNode

class CallOfSuperConstructorOverrideOnlyAllowedUsageFilter: ApiUsageFilter {
  override fun allow(invokedMethod: Method,
                     invocationInstruction: AbstractInsnNode,
                     callerMethod: Method,
                     context: VerificationContext): Boolean {
    return invokedMethod.isConstructor
        && callerMethod.isConstructor
        && callerMethod.containingClassFile.superName == invokedMethod.containingClassFile.name
  }
}