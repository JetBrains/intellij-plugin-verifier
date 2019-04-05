package com.jetbrains.pluginverifier.verifiers.instruction

import com.jetbrains.pluginverifier.results.problems.AbstractClassInstantiationProblem
import com.jetbrains.pluginverifier.results.problems.InterfaceInstantiationProblem
import com.jetbrains.pluginverifier.verifiers.*
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.TypeInsnNode

/**
 * Processing of NEW, ANEWARRAY, CHECKCAST and INSTANCEOF instructions.
 */
class TypeInstructionVerifier : InstructionVerifier {
  override fun verify(clazz: ClassNode, method: MethodNode, instr: AbstractInsnNode, ctx: VerificationContext) {
    if (instr !is TypeInsnNode) return

    val desc = instr.desc
    val className = desc.extractClassNameFromDescr() ?: return

    val aClass = ctx.resolveClassOrProblem(className, clazz, { createMethodLocation(clazz, method) }) ?: return

    if (instr.opcode == Opcodes.NEW) {
      if (aClass.isInterface()) {
        val interfaze = aClass.createClassLocation()
        ctx.registerProblem(InterfaceInstantiationProblem(interfaze, createMethodLocation(clazz, method)))
      } else if (aClass.isAbstract()) {
        val classOrInterface = aClass.createClassLocation()
        ctx.registerProblem(AbstractClassInstantiationProblem(classOrInterface, createMethodLocation(clazz, method)))
      }
    }

  }

}
