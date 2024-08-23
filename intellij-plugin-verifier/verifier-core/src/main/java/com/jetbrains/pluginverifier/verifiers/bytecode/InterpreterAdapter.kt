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

class InterpreterAdapter(private val interpreterListener: InterpreterListener = DefaultInterpreterListener) :
  Interpreter<BasicValue>(ASM9) {
  private val interpreter = BasicInterpreter()

  override fun newValue(type: Type?): BasicValue? {
    return interpreter.newValue(type)
  }

  override fun newOperation(insn: AbstractInsnNode?): BasicValue? {
    if (insn == null) throw AssertionError("Instruction cannot be null")
    if (insn.opcode == LDC) {
      return interpreterListener.onLdc(insn as LdcInsnNode, insn.cst, interpreter)
    }
    return when (insn.opcode) {
      in ICONST_M1..ICONST_5 -> insn.opcode - 3
      BIPUSH, SIPUSH -> (insn as IntInsnNode).operand
      else -> null
    }?.let { interpreterListener.onPushInt(insn, it, interpreter) }
      ?: interpreter.newOperation(insn)
  }

  override fun naryOperation(insn: AbstractInsnNode, values: MutableList<out BasicValue>?): BasicValue? {
    return if (insn.opcode == INVOKESPECIAL) {
      interpreterListener.onInvokeSpecial(insn as MethodInsnNode, values, interpreter)
    } else {
      interpreter.naryOperation(insn, values)
    }
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
