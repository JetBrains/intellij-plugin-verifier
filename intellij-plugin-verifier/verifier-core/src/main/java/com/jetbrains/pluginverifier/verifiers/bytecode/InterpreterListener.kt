package com.jetbrains.pluginverifier.verifiers.bytecode

import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.analysis.BasicInterpreter
import org.objectweb.asm.tree.analysis.BasicValue
import org.objectweb.asm.tree.analysis.Interpreter

interface InterpreterListener {
  fun onLdc(ldc: LdcInsnNode, value: Any, interpreter: Interpreter<BasicValue>): BasicValue? =
    interpreter.newOperation(ldc)

  fun onPushInt(insn: AbstractInsnNode, value: Int, interpreter: Interpreter<BasicValue>): BasicValue? =
    interpreter.newOperation(insn)

  fun onInvokeSpecial(
    invokeSpecial: MethodInsnNode,
    values: List<BasicValue>?,
    interpreter: BasicInterpreter
  ): BasicValue? =
    interpreter.naryOperation(invokeSpecial, values)
}

object DefaultInterpreterListener : InterpreterListener