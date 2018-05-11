package com.jetbrains.pluginverifier.results.problems

import com.jetbrains.pluginverifier.misc.formatMessage
import com.jetbrains.pluginverifier.results.instruction.Instruction
import com.jetbrains.pluginverifier.results.location.MethodLocation
import java.util.*

class InvokeNonStaticInstructionOnStaticMethodProblem(
    val resolvedMethod: MethodLocation,
    val caller: MethodLocation,
    val instruction: Instruction
) : CompatibilityProblem() {

  override val shortDescription = "Attempt to execute a non-static instruction *{0}* on a static method {1}".formatMessage(instruction, resolvedMethod)

  override val fullDescription = "Method {0} contains an *{1}* instruction referencing a static method {2}. This can lead to **IncompatibleClassChangeError** exception at runtime.".formatMessage(caller, instruction, resolvedMethod)

  override fun equals(other: Any?) = other is InvokeNonStaticInstructionOnStaticMethodProblem
      && resolvedMethod == other.resolvedMethod
      && caller == other.caller
      && instruction == other.instruction

  override fun hashCode() = Objects.hash(resolvedMethod, caller, instruction)
}