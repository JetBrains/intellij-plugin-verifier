package com.jetbrains.pluginverifier.results.problems

import com.jetbrains.pluginverifier.misc.formatMessage
import com.jetbrains.pluginverifier.results.instruction.Instruction
import com.jetbrains.pluginverifier.results.location.FieldLocation
import com.jetbrains.pluginverifier.results.location.MethodLocation

data class NonStaticAccessOfStaticFieldProblem(val field: FieldLocation,
                                               val accessor: MethodLocation,
                                               val instruction: Instruction) : CompatibilityProblem() {

  override val shortDescription = "Attempt to execute a non-static access instruction *{0}* on a static field {1}".formatMessage(instruction, field)

  override val fullDescription = "Method {0} has non-static access instruction *{1}* referencing a static field {2}. This can lead to **IncompatibleClassChangeError** exception at runtime.".formatMessage(accessor, instruction, field)

}