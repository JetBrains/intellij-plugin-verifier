package com.jetbrains.pluginverifier.verifiers.instruction

import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import org.objectweb.asm.tree.AbstractInsnNode

interface InstructionVerifier {
  fun verify(method: Method, instructionNode: AbstractInsnNode, context: VerificationContext)
}