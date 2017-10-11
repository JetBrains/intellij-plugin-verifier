package com.jetbrains.pluginverifier.results.problems

import com.jetbrains.pluginverifier.misc.formatMessage
import com.jetbrains.pluginverifier.results.instruction.Instruction
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.reference.FieldReference

data class FieldNotFoundProblem(val field: FieldReference,
                                val accessor: MethodLocation,
                                val instruction: Instruction) : Problem() {

  override val shortDescription = "Access to unresolved field {0}".formatMessage(field)

  override val fullDescription = "Method {0} contains a *{1}* instruction referencing an unresolved field {2}. This can lead to **NoSuchFieldError** exception at runtime.".formatMessage(accessor, instruction, field)
}