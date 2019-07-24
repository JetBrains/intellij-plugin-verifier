package com.jetbrains.pluginverifier.verifiers.instruction

import com.jetbrains.pluginverifier.results.instruction.Instruction
import com.jetbrains.pluginverifier.results.reference.FieldReference
import com.jetbrains.pluginverifier.results.reference.MethodReference
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.extractClassNameFromDescriptor
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import com.jetbrains.pluginverifier.verifiers.resolution.resolveClassChecked
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.MethodInsnNode

class MemberAccessVerifier : InstructionVerifier {
  override fun verify(method: Method, instructionNode: AbstractInsnNode, context: VerificationContext) {
    if (instructionNode is MethodInsnNode) {
      val instruction = Instruction.fromOpcode(instructionNode.opcode) ?: return
      verifyMemberAccess(method, instructionNode.owner, instructionNode.name, instructionNode.desc, instruction, context)
    }

    if (instructionNode is FieldInsnNode) {
      val instruction = Instruction.fromOpcode(instructionNode.opcode) ?: return
      verifyMemberAccess(method, instructionNode.owner, instructionNode.name, instructionNode.desc, instruction, context)
    }
  }

  private fun verifyMemberAccess(
      callerMethod: Method,
      memberOwner: String,
      memberName: String,
      memberDesc: String,
      instruction: Instruction,
      context: VerificationContext
  ) {
    if (memberOwner.startsWith("[")) {
      val arrayType = memberOwner.extractClassNameFromDescriptor()
      if (arrayType != null) {
        context.classResolver.resolveClassChecked(arrayType, callerMethod, context)
      }
      return
    }

    val ownerClassFile = context.classResolver.resolveClassChecked(memberOwner, callerMethod, context)
    if (ownerClassFile != null) {
      when (instruction) {
        Instruction.INVOKE_VIRTUAL, Instruction.INVOKE_INTERFACE, Instruction.INVOKE_STATIC, Instruction.INVOKE_SPECIAL -> {
          val methodReference = MethodReference(memberOwner, memberName, memberDesc)
          MethodInvokeInstructionVerifier(callerMethod, ownerClassFile, methodReference, context, instruction).verify()
        }

        Instruction.GET_STATIC, Instruction.PUT_STATIC, Instruction.PUT_FIELD, Instruction.GET_FIELD -> {
          val fieldReference = FieldReference(memberOwner, memberName, memberDesc)
          FieldAccessInstructionVerifier(callerMethod, ownerClassFile, fieldReference, context, instruction).verify()
        }
      }
    }
  }

}