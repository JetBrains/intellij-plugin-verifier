package com.jetbrains.pluginverifier.verifiers.instruction

import com.jetbrains.pluginverifier.problems.AbstractClassInstantiationProblem
import com.jetbrains.pluginverifier.problems.InterfaceInstantiationProblem
import com.jetbrains.pluginverifier.utils.VerificationContext
import com.jetbrains.pluginverifier.utils.VerifierUtil
import com.jetbrains.pluginverifier.utils.resolveClassOrProblem
import org.jetbrains.intellij.plugins.internal.asm.Opcodes
import org.jetbrains.intellij.plugins.internal.asm.tree.AbstractInsnNode
import org.jetbrains.intellij.plugins.internal.asm.tree.ClassNode
import org.jetbrains.intellij.plugins.internal.asm.tree.MethodNode
import org.jetbrains.intellij.plugins.internal.asm.tree.TypeInsnNode

/**
 * Processing of NEW, ANEWARRAY, CHECKCAST and INSTANCEOF instructions.

 * @author Dennis.Ushakov
 */
class TypeInstructionVerifier : InstructionVerifier {
  override fun verify(clazz: ClassNode, method: MethodNode, instr: AbstractInsnNode, ctx: VerificationContext) {
    if (instr !is TypeInsnNode) return

    val desc = instr.desc
    val className = VerifierUtil.extractClassNameFromDescr(desc) ?: return

    val aClass = ctx.resolveClassOrProblem(className, clazz, { ctx.fromMethod(clazz, method) }) ?: return

    if (instr.opcode == Opcodes.NEW) {
      if (VerifierUtil.isInterface(aClass)) {
        val interfaze = ctx.fromClass(aClass)
        ctx.registerProblem(InterfaceInstantiationProblem(interfaze, ctx.fromMethod(clazz, method)))
      } else if (VerifierUtil.isAbstract(aClass)) {
        val classOrInterface = ctx.fromClass(aClass)
        ctx.registerProblem(AbstractClassInstantiationProblem(classOrInterface, ctx.fromMethod(clazz, method)))
      }
    }

  }

}
