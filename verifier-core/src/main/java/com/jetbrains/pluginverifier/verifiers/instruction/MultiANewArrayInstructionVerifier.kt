package com.jetbrains.pluginverifier.verifiers.instruction

import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.checkClassExistsOrExternal
import com.jetbrains.pluginverifier.verifiers.extractClassNameFromDescr
import com.jetbrains.pluginverifier.verifiers.fromMethod
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.MultiANewArrayInsnNode

/**
 * @author Sergey Patrikeev
 */
class MultiANewArrayInstructionVerifier : InstructionVerifier {
  override fun verify(clazz: ClassNode, method: MethodNode, instr: AbstractInsnNode, ctx: VerificationContext) {
    if (instr !is MultiANewArrayInsnNode) return
    val descr = instr.desc.extractClassNameFromDescr() ?: return
    ctx.checkClassExistsOrExternal(descr, clazz, { ctx.fromMethod(clazz, method) })
  }
}
