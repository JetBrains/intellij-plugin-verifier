package com.jetbrains.pluginverifier.verifiers.instruction

import com.jetbrains.pluginverifier.results.problems.AbstractClassInstantiationProblem
import com.jetbrains.pluginverifier.results.problems.InterfaceInstantiationProblem
import com.jetbrains.pluginverifier.verifiers.BytecodeUtil
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.resolveClassOrProblem
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
  override fun verify(clazz: ClassNode, method: MethodNode, instr: AbstractInsnNode, ctx: VerificationContext) {
    if (instr !is TypeInsnNode) return

    val desc = instr.desc
    val className = BytecodeUtil.extractClassNameFromDescr(desc) ?: return

    val aClass = ctx.resolveClassOrProblem(className, clazz, { ctx.fromMethod(clazz, method) }) ?: return

    if (instr.opcode == Opcodes.NEW) {
      if (BytecodeUtil.isInterface(aClass)) {
        val interfaze = ctx.fromClass(aClass)
        ctx.registerProblem(InterfaceInstantiationProblem(interfaze, ctx.fromMethod(clazz, method)))
      } else if (BytecodeUtil.isAbstract(aClass)) {
        val classOrInterface = ctx.fromClass(aClass)
        ctx.registerProblem(AbstractClassInstantiationProblem(classOrInterface, ctx.fromMethod(clazz, method)))
      }
    }

  }

}
