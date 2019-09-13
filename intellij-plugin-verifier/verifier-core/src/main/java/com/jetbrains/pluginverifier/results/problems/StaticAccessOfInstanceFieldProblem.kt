package com.jetbrains.pluginverifier.results.problems

import com.jetbrains.plugin.structure.base.utils.formatMessage
import com.jetbrains.pluginverifier.results.instruction.Instruction
import com.jetbrains.pluginverifier.results.location.FieldLocation
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.reference.FieldReference
import java.util.*

class StaticAccessOfInstanceFieldProblem(
    val fieldReference: FieldReference,
    val field: FieldLocation,
    val accessor: MethodLocation,
    val instruction: Instruction
) : CompatibilityProblem() {

  override val problemType
    get() = "Static field changed to instance field"

  override val shortDescription
    get() = "Attempt to execute static access instruction *{0}* on instance field {1}".formatMessage(instruction, field)

  override val fullDescription
    get() = ("Method {0} has static field access instruction *{1}* referencing an instance field {2}, " +
        "what might have been caused by incompatible change of the field from static to instance. " +
        "This can lead to **IncompatibleClassChangeError** exception at runtime.").formatMessage(accessor, instruction, field)

  override fun equals(other: Any?) = other is StaticAccessOfInstanceFieldProblem
      && fieldReference == other.fieldReference
      && field == other.field
      && accessor == other.accessor
      && instruction == other.instruction

  override fun hashCode() = Objects.hash(fieldReference, field, accessor, instruction)
}