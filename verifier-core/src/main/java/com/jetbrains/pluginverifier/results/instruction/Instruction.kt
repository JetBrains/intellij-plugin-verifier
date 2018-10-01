package com.jetbrains.pluginverifier.results.instruction

import org.objectweb.asm.Opcodes

enum class Instruction(private val type: String) {
  GET_STATIC("getstatic"),
  PUT_STATIC("putstatic"),
  PUT_FIELD("putfield"),
  GET_FIELD("getfield"),
  INVOKE_VIRTUAL("invokevirtual"),
  INVOKE_INTERFACE("invokeinterface"),
  INVOKE_STATIC("invokestatic"),
  INVOKE_SPECIAL("invokespecial");

  override fun toString(): String = type

  companion object {
    fun fromOpcode(opcode: Int): Instruction? = when (opcode) {
      Opcodes.INVOKEVIRTUAL -> Instruction.INVOKE_VIRTUAL
      Opcodes.INVOKESPECIAL -> Instruction.INVOKE_SPECIAL
      Opcodes.INVOKEINTERFACE -> Instruction.INVOKE_INTERFACE
      Opcodes.INVOKESTATIC -> Instruction.INVOKE_STATIC

      Opcodes.PUTFIELD -> Instruction.PUT_FIELD
      Opcodes.GETFIELD -> Instruction.GET_FIELD
      Opcodes.PUTSTATIC -> Instruction.PUT_STATIC
      Opcodes.GETSTATIC -> Instruction.GET_STATIC
      else -> null
    }
  }

}