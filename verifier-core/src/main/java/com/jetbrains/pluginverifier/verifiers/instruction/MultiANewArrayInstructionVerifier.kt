package com.jetbrains.pluginverifier.verifiers.instruction

import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.VContext
import com.jetbrains.pluginverifier.location.ProblemLocation
import com.jetbrains.pluginverifier.problems.ClassNotFoundProblem
import com.jetbrains.pluginverifier.utils.VerifierUtil
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.MultiANewArrayInsnNode

/**
 * @author Sergey Patrikeev
 */
class MultiANewArrayInstructionVerifier : InstructionVerifier {
  override fun verify(clazz: ClassNode, method: MethodNode, instr: AbstractInsnNode, resolver: Resolver, ctx: VContext) {
    if (instr !is MultiANewArrayInsnNode) return
    val descr = VerifierUtil.extractClassNameFromDescr(instr.desc) ?: return
    if (!VerifierUtil.classExistsOrExternal(ctx, resolver, descr)) {
      ctx.registerProblem(ClassNotFoundProblem(descr), ProblemLocation.fromMethod(clazz.name, method))
    }
  }
}
