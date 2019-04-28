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
      Opcodes.INVOKEVIRTUAL -> INVOKE_VIRTUAL
      Opcodes.INVOKESPECIAL -> INVOKE_SPECIAL
      Opcodes.INVOKEINTERFACE -> INVOKE_INTERFACE
      Opcodes.INVOKESTATIC -> INVOKE_STATIC

      Opcodes.PUTFIELD -> PUT_FIELD
      Opcodes.GETFIELD -> GET_FIELD
      Opcodes.PUTSTATIC -> PUT_STATIC
      Opcodes.GETSTATIC -> GET_STATIC
      else -> null
    }
  }

}