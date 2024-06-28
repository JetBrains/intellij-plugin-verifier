package com.jetbrains.pluginverifier.verifiers.filter

import com.jetbrains.pluginverifier.results.reference.ClassReference
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileMember
import com.jetbrains.pluginverifier.verifiers.resolution.ClassUsageType
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import org.objectweb.asm.tree.AbstractInsnNode

interface ApiUsageFilter {
  fun allowMethodInvocation(invokedMethod: Method,
                            invocationInstruction: AbstractInsnNode,
                            callerMethod: Method,
                            context: VerificationContext): Boolean = true

  fun allowUsage(
    classReference: ClassReference,
    invocationTarget: ClassFile,
    caller: ClassFileMember,
    usageType: ClassUsageType,
    context: VerificationContext
  ): Boolean = true

}