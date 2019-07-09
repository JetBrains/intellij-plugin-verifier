package com.jetbrains.pluginverifier.verifiers.instruction

import com.jetbrains.pluginverifier.results.problems.AbstractClassInstantiationProblem
import com.jetbrains.pluginverifier.results.problems.InterfaceInstantiationProblem
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.extractClassNameFromDescriptor
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import com.jetbrains.pluginverifier.verifiers.resolution.resolveClassChecked
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.TypeInsnNode

/**
 * Processing of `new`, `anewarray`, `checkcast`, `instanceof` instructions.
 */
class TypeInstructionVerifier : InstructionVerifier {
  override fun verify(method: Method, instructionNode: AbstractInsnNode, context: VerificationContext) {
    if (instructionNode !is TypeInsnNode) return

    val className = instructionNode.desc.extractClassNameFromDescriptor() ?: return

    val typeClassFile = context.classResolver.resolveClassChecked(className, method, context) ?: return

    if (instructionNode.opcode == Opcodes.NEW) {
      if (typeClassFile.isInterface) {
        context.problemRegistrar.registerProblem(InterfaceInstantiationProblem(typeClassFile.location, method.location))
      } else if (typeClassFile.isAbstract) {
        context.problemRegistrar.registerProblem(AbstractClassInstantiationProblem(typeClassFile.location, method.location))
      }
    }

  }

}
