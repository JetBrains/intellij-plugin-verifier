package com.jetbrains.pluginverifier.verifiers.bytecode

import com.jetbrains.pluginverifier.verifiers.resolution.BinaryClassName
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.analysis.BasicInterpreter
import org.objectweb.asm.tree.analysis.BasicValue
import org.objectweb.asm.tree.analysis.Interpreter

class InvokeSpecialInterpreterListener : InterpreterListener {
  private val _invocations = mutableListOf<Invocation>()
  val invocations: List<Invocation>
    get() = _invocations

  override fun onInvokeSpecial(
    invokeSpecial: MethodInsnNode,
    values: List<BasicValue>?,
    interpreter: BasicInterpreter
  ): BasicValue? {
    _invocations += Invocation(invokeSpecial, values.orEmpty())
    return super.onInvokeSpecial(invokeSpecial, values, interpreter)
  }

  override fun onLdc(ldc: LdcInsnNode, value: Any, interpreter: Interpreter<BasicValue>): BasicValue? {
    return when (value) {
      is String -> StringValue(value)
      is Int -> IntValue(value)
      else -> interpreter.newOperation(ldc)
    }
  }

  override fun onPushInt(
    insn: AbstractInsnNode,
    value: Int,
    interpreter: Interpreter<BasicValue>
  ) = IntValue(value)

  data class Invocation(val invokeSpecial: MethodInsnNode, val values: List<BasicValue>) {
    val invocationTarget: BinaryClassName
      get() = invokeSpecial.owner

    val methodName: String
      get() = invokeSpecial.name

    val desc: String
      get() = invokeSpecial.desc

  }
}