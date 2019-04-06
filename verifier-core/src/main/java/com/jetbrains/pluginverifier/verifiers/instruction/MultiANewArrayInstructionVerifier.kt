package com.jetbrains.pluginverifier.verifiers.instruction

import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.createMethodLocation
import com.jetbrains.pluginverifier.verifiers.extractClassNameFromDescr
import com.jetbrains.pluginverifier.verifiers.resolveClassOrProblem
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.MultiANewArrayInsnNode

class MultiANewArrayInstructionVerifier : InstructionVerifier {
  override fun verify(clazz: ClassNode, method: MethodNode, instr: AbstractInsnNode, ctx: VerificationContext) {
    if (instr !is MultiANewArrayInsnNode) return
    val descr = instr.desc.extractClassNameFromDescr() ?: return

    //During resolution of the symbolic reference to the class, array, or interface type,
    // any of the exceptions documented in §5.4.3.1 can be thrown.
    ctx.resolveClassOrProblem(descr, clazz) { createMethodLocation(clazz, method) }
  }
}
