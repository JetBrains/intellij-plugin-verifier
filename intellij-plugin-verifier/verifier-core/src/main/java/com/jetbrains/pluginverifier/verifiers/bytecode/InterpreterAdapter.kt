package com.jetbrains.pluginverifier.verifiers.bytecode

import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.IntInsnNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.analysis.BasicInterpreter
import org.objectweb.asm.tree.analysis.BasicValue
import org.objectweb.asm.tree.analysis.Interpreter


typealias Opcode = Int
/**
 * ASM Interpreter that offloads specific instruction to typesafe [handler](InterpreterListener).
 */
class InterpreterAdapter(private val interpreterListener: InterpreterListener = DefaultInterpreterListener) :
  Interpreter<BasicValue>(ASM9) {
  private val interpreter = BasicInterpreter()

  private val singleIntOpcodes: List<Opcode> = (ICONST_M1..ICONST_5) + listOf(BIPUSH, SIPUSH)

  /**
   * Type-safe handling of:
   *
   * - integer handling instructions: `LDC`, `ICONST_x`, `BIPUSH` and `SIPUSH
   * - `LDC` instruction with typesafe handling of operand
   */
  override fun newOperation(insn: AbstractInsnNode?): BasicValue? {
    if (insn == null) throw AssertionError("Instruction cannot be null")
    return when(insn.opcode) {
      LDC -> interpreterListener.onLdc(insn as LdcInsnNode, insn.cst, interpreter)
      in singleIntOpcodes -> insn.operand()?.let {
        interpreterListener.onPushInt(insn, it, interpreter) }
      else -> interpreter.newOperation(insn)
    }
  }

  /**
   * Type-safe handling of:
   *
   * - `INVOKESPECIAL` with type-safe passing of stack values
   */
  override fun naryOperation(insn: AbstractInsnNode, values: MutableList<out BasicValue>?): BasicValue? {
    return if (insn.opcode == INVOKESPECIAL) {
      interpreterListener.onInvokeSpecial(insn as MethodInsnNode, values, interpreter)
    } else {
      interpreter.naryOperation(insn, values)
    }
  }

  /**
   * Extract a single operand as an integer.
   *
   * @return the integer operand or `null` when the instruction does not provide a single integer-like operand.
   */
  private fun AbstractInsnNode.operand(): Int? = when (opcode) {
    in ICONST_M1..ICONST_5 -> opcode - 3
    BIPUSH, SIPUSH -> (this as IntInsnNode).operand
    else -> null
  }

  // --------------------------------------------------------------------------------------------

  override fun newValue(type: Type?): BasicValue? {
    return interpreter.newValue(type)
  }
  override fun copyOperation(insn: AbstractInsnNode?, value: BasicValue?): BasicValue? =
    interpreter.copyOperation(insn, value)

  override fun unaryOperation(insn: AbstractInsnNode?, value: BasicValue?): BasicValue? =
    interpreter.unaryOperation(insn, value)

  override fun binaryOperation(insn: AbstractInsnNode?, value1: BasicValue?, value2: BasicValue?): BasicValue? =
    interpreter.binaryOperation(insn, value1, value2)

  override fun ternaryOperation(
    insn: AbstractInsnNode?,
    value1: BasicValue?,
    value2: BasicValue?,
    value3: BasicValue?
  ): BasicValue? = interpreter.ternaryOperation(insn, value1, value2, value3)

  override fun returnOperation(insn: AbstractInsnNode?, value: BasicValue?, expected: BasicValue?) =
    interpreter.returnOperation(insn, value, expected)

  override fun merge(value1: BasicValue?, value2: BasicValue?): BasicValue? = interpreter.merge(value1, value2)
}
