package com.jetbrains.pluginverifier.verifiers.instruction

import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.VContext
import com.jetbrains.pluginverifier.utils.VerifierUtil
import org.jetbrains.intellij.plugins.internal.asm.Type
import org.jetbrains.intellij.plugins.internal.asm.tree.AbstractInsnNode
import org.jetbrains.intellij.plugins.internal.asm.tree.ClassNode
import org.jetbrains.intellij.plugins.internal.asm.tree.LdcInsnNode
import org.jetbrains.intellij.plugins.internal.asm.tree.MethodNode

class LdcInstructionVerifier : InstructionVerifier {
  override fun verify(clazz: ClassNode, method: MethodNode, instr: AbstractInsnNode, resolver: Resolver, ctx: VContext) {
    if (instr !is LdcInsnNode) return

    val constant = instr.cst
    if (constant !is Type) return

    val descriptor = constant.descriptor
    val className = VerifierUtil.extractClassNameFromDescr(descriptor) ?: return

    VerifierUtil.checkClassExistsOrExternal(resolver, className, ctx, { ctx.fromMethod(clazz.name, method) })
  }
}
