package com.jetbrains.pluginverifier.results.problems

import com.jetbrains.plugin.structure.base.utils.formatMessage
import com.jetbrains.pluginverifier.results.instruction.Instruction
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.reference.MethodReference
import java.util.*

class InvokeInstanceInstructionOnStaticMethodProblem(
  val methodReference: MethodReference,
  val resolvedMethod: MethodLocation,
  val caller: MethodLocation,
  val instruction: Instruction
) : CompatibilityProblem() {

  override val problemType
    get() = "Instance method changed to static method"

  override val shortDescription
    get() = "Attempt to execute instance instruction *{0}* on a static method {1}".formatMessage(instruction, resolvedMethod)

  override val fullDescription
    get() = ("Method {0} contains an *{1}* instruction referencing a static method {2}, " +
      "what might have been caused by incompatible change of the method to static. " +
      "This can lead to **IncompatibleClassChangeError** exception at runtime."
      ).formatMessage(caller, instruction, resolvedMethod)

  override fun equals(other: Any?) = other is InvokeInstanceInstructionOnStaticMethodProblem
    && methodReference == other.methodReference
    && resolvedMethod == other.resolvedMethod
    && caller == other.caller
    && instruction == other.instruction

  override fun hashCode() = Objects.hash(methodReference, resolvedMethod, caller, instruction)
}