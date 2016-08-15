package com.jetbrains.pluginverifier.verifiers.instruction

import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.VContext
import com.jetbrains.pluginverifier.location.ProblemLocation
import com.jetbrains.pluginverifier.problems.AbstractClassInstantiationProblem
import com.jetbrains.pluginverifier.problems.InterfaceInstantiationProblem
import com.jetbrains.pluginverifier.utils.VerifierUtil
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.TypeInsnNode

/**
 * Processing of NEW, ANEWARRAY, CHECKCAST and INSTANCEOF instructions.

 * @author Dennis.Ushakov
 */
class TypeInstructionVerifier : InstructionVerifier {
  override fun verify(clazz: ClassNode, method: MethodNode, instr: AbstractInsnNode, resolver: Resolver, ctx: VContext) {
    if (instr !is TypeInsnNode) return

    val desc = instr.desc
    val className = VerifierUtil.extractClassNameFromDescr(desc) ?: return

    val aClass = VerifierUtil.resolveClassOrProblem(resolver, className, clazz, ctx, { ProblemLocation.fromMethod(clazz.name, method) }) ?: return

    if (instr.opcode == Opcodes.NEW) {
      if (VerifierUtil.isInterface(aClass)) {
        ctx.registerProblem(InterfaceInstantiationProblem(className), ProblemLocation.fromMethod(clazz.name, method))
      } else if (VerifierUtil.isAbstract(aClass)) {
        ctx.registerProblem(AbstractClassInstantiationProblem(className), ProblemLocation.fromMethod(clazz.name, method))
      }
    }

  }

}
