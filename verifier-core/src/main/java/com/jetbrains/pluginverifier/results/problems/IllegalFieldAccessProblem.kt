package com.jetbrains.pluginverifier.results.problems

import com.jetbrains.pluginverifier.misc.formatMessage
import com.jetbrains.pluginverifier.results.access.AccessType
import com.jetbrains.pluginverifier.results.instruction.Instruction
import com.jetbrains.pluginverifier.results.location.FieldLocation
import com.jetbrains.pluginverifier.results.location.MethodLocation

data class IllegalFieldAccessProblem(val field: FieldLocation,
                                     val accessor: MethodLocation,
                                     val instruction: Instruction,
                                     val fieldAccess: AccessType) : Problem() {

  override val shortDescription = "Illegal access to a {0} field {1}".formatMessage(fieldAccess, field)

  override val fullDescription = "Method {0} contains a *{1}* instruction referencing a {2} field {3} that a class {4} doesn''t have access to. This can lead to **IllegalAccessError** exception at runtime.".formatMessage(accessor, instruction, fieldAccess, field, accessor.hostClass)

}