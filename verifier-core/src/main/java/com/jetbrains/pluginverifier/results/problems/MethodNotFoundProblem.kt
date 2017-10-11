package com.jetbrains.pluginverifier.results.problems

import com.jetbrains.pluginverifier.misc.formatMessage
import com.jetbrains.pluginverifier.results.instruction.Instruction
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.reference.MethodReference

data class MethodNotFoundProblem(val method: MethodReference,
                                 val caller: MethodLocation,
                                 val instruction: Instruction) : Problem() {

  override val shortDescription = "Invocation of unresolved method {0}".formatMessage(method)

  override val fullDescription = "Method {0} contains an *{1}* instruction referencing an unresolved method {2}. This can lead to **NoSuchMethodError** exception at runtime.".formatMessage(caller, instruction, method)

}