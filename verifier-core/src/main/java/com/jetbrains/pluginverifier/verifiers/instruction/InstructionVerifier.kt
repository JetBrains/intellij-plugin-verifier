package com.jetbrains.pluginverifier.verifiers.instruction

import com.jetbrains.pluginverifier.verifiers.VerificationContext
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

interface InstructionVerifier {
  fun verify(clazz: ClassNode, method: MethodNode, instr: AbstractInsnNode, ctx: VerificationContext)
}