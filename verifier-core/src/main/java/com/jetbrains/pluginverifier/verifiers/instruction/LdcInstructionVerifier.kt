package com.jetbrains.pluginverifier.verifiers.instruction

import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.createMethodLocation
import com.jetbrains.pluginverifier.verifiers.extractClassNameFromDescr
import com.jetbrains.pluginverifier.verifiers.resolveClassOrProblem
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodNode

class LdcInstructionVerifier : InstructionVerifier {
  override fun verify(clazz: ClassNode, method: MethodNode, instr: AbstractInsnNode, ctx: VerificationContext) {
    if (instr !is LdcInsnNode) return

    val constant = instr.cst as? Type ?: return

    val className = constant.descriptor.extractClassNameFromDescr() ?: return

    //Otherwise, if the run-time constant pool entry is a symbolic reference to a class (§5.1),
    // then the named class is resolved (§5.4.3.1)
    //During resolution of a symbolic reference to a class, any of the exceptions pertaining
    // to class resolution (§5.4.3.1) can be thrown.
    ctx.resolveClassOrProblem(className, clazz) { createMethodLocation(clazz, method) }
  }
}
