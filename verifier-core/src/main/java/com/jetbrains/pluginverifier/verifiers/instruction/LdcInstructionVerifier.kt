package com.jetbrains.pluginverifier.verifiers.instruction

import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.VContext
import com.jetbrains.pluginverifier.location.ProblemLocation
import com.jetbrains.pluginverifier.utils.VerifierUtil
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodNode

class LdcInstructionVerifier : InstructionVerifier {
  override fun verify(clazz: ClassNode, method: MethodNode, instr: AbstractInsnNode, resolver: Resolver, ctx: VContext) {
    if (instr !is LdcInsnNode) return

    val constant = instr.cst
    if (constant !is Type) return

    val descriptor = constant.descriptor
    val className = VerifierUtil.extractClassNameFromDescr(descriptor) ?: return

    VerifierUtil.checkClassExistsOrExternal(resolver, className, ctx, { ProblemLocation.fromMethod(clazz.name, method) })
  }
}
