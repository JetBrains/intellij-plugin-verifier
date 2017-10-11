package com.jetbrains.pluginverifier.results.problems

import com.jetbrains.pluginverifier.misc.formatMessage
import com.jetbrains.pluginverifier.results.instruction.Instruction
import com.jetbrains.pluginverifier.results.location.FieldLocation
import com.jetbrains.pluginverifier.results.location.MethodLocation

data class StaticAccessOfNonStaticFieldProblem(val field: FieldLocation,
                                               val accessor: MethodLocation,
                                               val instruction: Instruction) : Problem() {

  override val shortDescription = "Attempt to execute a static access instruction *{0}* on a non-static field {1}".formatMessage(instruction, field)

  override val fullDescription = "Method {0} has static access instruction *{1}* referencing a non-static field {2}. This can lead to **IncompatibleClassChangeError** exception at runtime.".formatMessage(accessor, instruction, field)
}