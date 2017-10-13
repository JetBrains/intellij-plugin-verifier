package com.jetbrains.pluginverifier.verifiers.instruction

import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.checkClassExistsOrExternal
import com.jetbrains.pluginverifier.verifiers.extractClassNameFromDescr
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodNode

class LdcInstructionVerifier : InstructionVerifier {
  override fun verify(clazz: ClassNode, method: MethodNode, instr: AbstractInsnNode, ctx: VerificationContext) {
    if (instr !is LdcInsnNode) return

    val constant = instr.cst as? Type ?: return

    val descriptor = constant.descriptor
    val className = descriptor.extractClassNameFromDescr() ?: return

    ctx.checkClassExistsOrExternal(className, clazz, { ctx.fromMethod(clazz, method) })
  }
}
