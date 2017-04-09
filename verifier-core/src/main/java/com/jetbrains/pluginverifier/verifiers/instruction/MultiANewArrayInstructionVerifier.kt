package com.jetbrains.pluginverifier.verifiers.instruction

import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.utils.VerificationContext
import com.jetbrains.pluginverifier.utils.VerifierUtil
import org.jetbrains.intellij.plugins.internal.asm.tree.AbstractInsnNode
import org.jetbrains.intellij.plugins.internal.asm.tree.ClassNode
import org.jetbrains.intellij.plugins.internal.asm.tree.MethodNode
import org.jetbrains.intellij.plugins.internal.asm.tree.MultiANewArrayInsnNode

/**
 * @author Sergey Patrikeev
 */
class MultiANewArrayInstructionVerifier : InstructionVerifier {
  override fun verify(clazz: ClassNode, method: MethodNode, instr: AbstractInsnNode, resolver: Resolver, ctx: VerificationContext) {
    if (instr !is MultiANewArrayInsnNode) return
    val descr = VerifierUtil.extractClassNameFromDescr(instr.desc) ?: return
    VerifierUtil.checkClassExistsOrExternal(resolver, descr, ctx, { ctx.fromMethod(clazz, method) })
  }
}
