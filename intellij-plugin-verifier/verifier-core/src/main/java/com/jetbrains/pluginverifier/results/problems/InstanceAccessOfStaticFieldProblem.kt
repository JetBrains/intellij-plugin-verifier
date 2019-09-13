package com.jetbrains.pluginverifier.results.problems

import com.jetbrains.plugin.structure.base.utils.formatMessage
import com.jetbrains.pluginverifier.results.instruction.Instruction
import com.jetbrains.pluginverifier.results.location.FieldLocation
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.reference.FieldReference
import java.util.*

class InstanceAccessOfStaticFieldProblem(
    val fieldReference: FieldReference,
    val field: FieldLocation,
    val accessor: MethodLocation,
    val instruction: Instruction
) : CompatibilityProblem() {

  override val problemType
    get() = "Instance field changed to static field"

  override val shortDescription
    get() = "Attempt to execute instance access instruction *{0}* on static field {1}".formatMessage(instruction, field)

  override val fullDescription
    get() = ("Method {0} has instance field access instruction *{1}* referencing static field {2}, " +
        "what might have been caused by incompatible change of the field to static. " +
        "This can lead to **IncompatibleClassChangeError** exception at runtime."
        ).formatMessage(accessor, instruction, field)

  override fun equals(other: Any?) = other is InstanceAccessOfStaticFieldProblem
      && fieldReference == other.fieldReference
      && field == other.field
      && accessor == other.accessor

  override fun hashCode() = Objects.hash(fieldReference, field, accessor)


}