package com.jetbrains.pluginverifier.results.problems

import com.jetbrains.pluginverifier.misc.formatMessage
import com.jetbrains.pluginverifier.results.instruction.Instruction
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.reference.MethodReference

data class MultipleDefaultImplementationsProblem(val caller: MethodLocation,
                                                 val methodReference: MethodReference,
                                                 val instruction: Instruction,
                                                 val implementation1: MethodLocation,
                                                 val implementation2: MethodLocation) : Problem() {

  override val shortDescription = "Multiple default implementations of method {0}".formatMessage(methodReference)

  override val fullDescription = "Method {0} contains an *{1}* instruction referencing a method reference {2} which has multiple default implementations: {3} and {4}. This can lead to **IncompatibleClassChangeError** exception at runtime.".formatMessage(caller, instruction, methodReference, implementation1, implementation2)

}