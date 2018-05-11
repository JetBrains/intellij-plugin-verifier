package com.jetbrains.pluginverifier.results.problems

import com.jetbrains.pluginverifier.misc.formatMessage
import com.jetbrains.pluginverifier.results.instruction.Instruction
import com.jetbrains.pluginverifier.results.location.FieldLocation
import com.jetbrains.pluginverifier.results.location.MethodLocation
import java.util.*

class StaticAccessOfNonStaticFieldProblem(
    val field: FieldLocation,
    val accessor: MethodLocation,
    val instruction: Instruction
) : CompatibilityProblem() {

  override val shortDescription = "Attempt to execute a static access instruction *{0}* on a non-static field {1}".formatMessage(instruction, field)

  override val fullDescription = "Method {0} has static access instruction *{1}* referencing a non-static field {2}. This can lead to **IncompatibleClassChangeError** exception at runtime.".formatMessage(accessor, instruction, field)

  override fun equals(other: Any?) = other is StaticAccessOfNonStaticFieldProblem
      && field == other.field
      && accessor == other.accessor
      && instruction == other.instruction

  override fun hashCode() = Objects.hash(field, accessor, instruction)
}