package com.jetbrains.pluginverifier.verifiers.instruction

import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.VContext
import com.jetbrains.pluginverifier.location.ProblemLocation
import com.jetbrains.pluginverifier.problems.ClassNotFoundProblem
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
    val className = VerifierUtil.extractClassNameFromDescr(descriptor)

    if (className == null || VerifierUtil.classExistsOrExternal(ctx, resolver, className)) return

    ctx.registerProblem(ClassNotFoundProblem(className), ProblemLocation.fromMethod(clazz.name, method))

    //TODO: process method handle Type: org.objectweb.asm.Type.METHOD
  }
}
