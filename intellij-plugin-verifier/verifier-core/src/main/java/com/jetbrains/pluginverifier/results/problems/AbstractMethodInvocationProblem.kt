package com.jetbrains.pluginverifier.results.problems

import com.jetbrains.plugin.structure.base.utils.formatMessage
import com.jetbrains.pluginverifier.results.instruction.Instruction
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.reference.MethodReference
import java.util.*

class AbstractMethodInvocationProblem(
  val bytecodeMethodReference: MethodReference,
  val method: MethodLocation,
  val caller: MethodLocation,
  val instruction: Instruction
) : CompatibilityProblem() {

  override val problemType
    get() = "Abstract method invocation"

  override val shortDescription
    get() = "Attempt to invoke an abstract method {0}".formatMessage(method)

  override val fullDescription
    get() = "Method {0} contains an *{1}* instruction referencing a method {2} which doesn''t have an implementation. This can lead to **AbstractMethodError** exception at runtime.".formatMessage(caller, instruction, method)

  override fun equals(other: Any?) = other is AbstractMethodInvocationProblem
    && bytecodeMethodReference == other.bytecodeMethodReference
    && method == other.method
    && caller == other.caller
    && instruction == other.instruction

  override fun hashCode() = Objects.hash(bytecodeMethodReference, method, caller, instruction)

}