package com.jetbrains.pluginverifier.verifiers.filter

import com.jetbrains.pluginverifier.results.reference.MethodReference
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import org.objectweb.asm.tree.AbstractInsnNode

interface ApiUsageFilter {
  fun allowMethodInvocation(methodReference: MethodReference,
                            resolvedMethod: Method,
                            instructionNode: AbstractInsnNode,
                            callerMethod: Method,
                            context: VerificationContext): Boolean = true
}