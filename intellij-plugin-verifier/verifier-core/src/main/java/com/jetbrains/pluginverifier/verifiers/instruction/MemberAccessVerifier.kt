/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.verifiers.instruction

import com.jetbrains.pluginverifier.results.instruction.Instruction
import com.jetbrains.pluginverifier.results.reference.FieldReference
import com.jetbrains.pluginverifier.results.reference.MethodReference
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.extractClassNameFromDescriptor
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import com.jetbrains.pluginverifier.verifiers.resolution.resolveClassChecked
import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.InvokeDynamicInsnNode
import org.objectweb.asm.tree.MethodInsnNode

class MemberAccessVerifier : InstructionVerifier {
  override fun verify(method: Method, instructionNode: AbstractInsnNode, context: VerificationContext) {
    if (instructionNode is MethodInsnNode) {
      val instruction = when (instructionNode.opcode) {
        Opcodes.INVOKEVIRTUAL -> Instruction.INVOKE_VIRTUAL
        Opcodes.INVOKESPECIAL -> Instruction.INVOKE_SPECIAL
        Opcodes.INVOKEINTERFACE -> Instruction.INVOKE_INTERFACE
        Opcodes.INVOKESTATIC -> Instruction.INVOKE_STATIC
        else -> return
      }
      verifyMemberAccess(method, instructionNode.owner, instructionNode.name, instructionNode.desc, instructionNode, instruction, context)
    }

    if (instructionNode is FieldInsnNode) {
      val instruction = when (instructionNode.opcode) {
        Opcodes.PUTFIELD -> Instruction.PUT_FIELD
        Opcodes.GETFIELD -> Instruction.GET_FIELD
        Opcodes.PUTSTATIC -> Instruction.PUT_STATIC
        Opcodes.GETSTATIC -> Instruction.GET_STATIC
        else -> return
      }
      verifyMemberAccess(method, instructionNode.owner, instructionNode.name, instructionNode.desc, instructionNode, instruction, context)
    }

    if (instructionNode is InvokeDynamicInsnNode) {
      processInvokeDynamic(method, instructionNode, context)
    }
  }

  private fun processInvokeDynamic(
    callerMethod: Method,
    instructionNode: InvokeDynamicInsnNode,
    context: VerificationContext
  ) {
    for (bsmArg in instructionNode.bsmArgs) {
      if (bsmArg is Handle) {
        val instruction = when (bsmArg.tag) {
          Opcodes.H_INVOKEVIRTUAL -> Instruction.INVOKE_VIRTUAL
          Opcodes.H_INVOKESPECIAL -> Instruction.INVOKE_SPECIAL
          Opcodes.H_INVOKEINTERFACE -> Instruction.INVOKE_INTERFACE
          Opcodes.H_INVOKESTATIC -> Instruction.INVOKE_STATIC

          Opcodes.H_PUTFIELD -> Instruction.PUT_FIELD
          Opcodes.H_GETFIELD -> Instruction.GET_FIELD
          Opcodes.H_PUTSTATIC -> Instruction.PUT_STATIC
          Opcodes.H_GETSTATIC -> Instruction.GET_STATIC
          else -> null
        } ?: continue

        verifyMemberAccess(callerMethod, bsmArg.owner, bsmArg.name, bsmArg.desc, instructionNode, instruction, context)
      }
    }
  }


  private fun verifyMemberAccess(
    callerMethod: Method,
    memberOwner: String,
    memberName: String,
    memberDesc: String,
    instructionNode: AbstractInsnNode,
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
          MethodInvokeInstructionVerifier(callerMethod, ownerClassFile, methodReference, context, instruction, instructionNode).verify()
        }

        Instruction.GET_STATIC, Instruction.PUT_STATIC, Instruction.PUT_FIELD, Instruction.GET_FIELD -> {
          val fieldReference = FieldReference(memberOwner, memberName, memberDesc)
          FieldAccessInstructionVerifier(callerMethod, ownerClassFile, fieldReference, context, instruction).verify()
        }
      }
    }
  }

}