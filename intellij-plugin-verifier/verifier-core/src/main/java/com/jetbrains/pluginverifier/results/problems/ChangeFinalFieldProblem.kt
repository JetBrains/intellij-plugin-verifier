package com.jetbrains.pluginverifier.results.problems

import com.jetbrains.plugin.structure.base.utils.formatMessage
import com.jetbrains.pluginverifier.results.instruction.Instruction
import com.jetbrains.pluginverifier.results.location.FieldLocation
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.reference.FieldReference
import java.util.*

class ChangeFinalFieldProblem(
  val fieldReference: FieldReference,
  val field: FieldLocation,
  val accessor: MethodLocation,
  val instruction: Instruction
) : CompatibilityProblem() {

  override val problemType
    get() = "Changing final field"

  override val shortDescription
    get() = "Attempt to change a final field {0}".formatMessage(field)

  override val fullDescription
    get() = "Method {0} has modifying instruction *{1}* referencing a final field {2}. This can lead to **IllegalAccessError** exception at runtime.".formatMessage(accessor, instruction, field)

  override fun equals(other: Any?) = other is ChangeFinalFieldProblem
    && fieldReference == other.fieldReference
    && field == other.field
    && accessor == other.accessor
    && instruction == other.instruction

  override fun hashCode() = Objects.hash(fieldReference, field, accessor, instruction)

}