package com.jetbrains.pluginverifier.verifiers.instruction

import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.createMethodLocation
import com.jetbrains.pluginverifier.verifiers.extractClassNameFromDescr
import com.jetbrains.pluginverifier.verifiers.resolveClassOrProblem
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodNode

class LdcInstructionVerifier : InstructionVerifier {
  override fun verify(clazz: ClassNode, method: MethodNode, instr: AbstractInsnNode, ctx: VerificationContext) {
    if (instr !is LdcInsnNode) return

    val lookupLocation = { createMethodLocation(clazz, method) }
    val type = instr.cst as? Type ?: return
    if (type.sort == Type.OBJECT) {
      checkTypeExists(type, ctx, clazz, lookupLocation)
    } else if (type.sort == Type.METHOD) {
      for (argumentType in type.argumentTypes) {
        checkTypeExists(argumentType, ctx, clazz, lookupLocation)
      }
      checkTypeExists(type.returnType, ctx, clazz, lookupLocation)
    }
  }

  private fun checkTypeExists(type: Type, ctx: VerificationContext, clazz: ClassNode, lookupLocation: () -> Location) {
    val className = type.descriptor.extractClassNameFromDescr() ?: return
    ctx.resolveClassOrProblem(className, clazz, lookupLocation)
  }
}
