package com.jetbrains.pluginverifier.results.problems

import com.jetbrains.pluginverifier.misc.formatMessage
import com.jetbrains.pluginverifier.results.instruction.Instruction
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.reference.MethodReference

data class InvokeClassMethodOnInterfaceProblem(val methodReference: MethodReference,
                                               val caller: MethodLocation,
                                               val instruction: Instruction) : CompatibilityProblem() {

  override val shortDescription = "Incompatible change of class {0} to interface".formatMessage(methodReference.hostClass)

  override val fullDescription = "Method {0} has invocation *{1}* instruction referencing a *class* method {2}, but the method''s host {3} is an *interface*. This can lead to **IncompatibleClassChangeError** at runtime.".formatMessage(caller, instruction, methodReference, methodReference.hostClass)

}



