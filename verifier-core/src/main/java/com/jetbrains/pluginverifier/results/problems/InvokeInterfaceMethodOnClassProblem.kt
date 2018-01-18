package com.jetbrains.pluginverifier.results.problems

import com.jetbrains.pluginverifier.misc.formatMessage
import com.jetbrains.pluginverifier.results.instruction.Instruction
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.reference.MethodReference

data class InvokeInterfaceMethodOnClassProblem(val methodReference: MethodReference,
                                               val caller: MethodLocation,
                                               val instruction: Instruction) : CompatibilityProblem() {

  override val shortDescription = "Incompatible change of interface {0} to class".formatMessage(methodReference.hostClass)

  override val fullDescription = "Method {0} has invocation *{1}* instruction referencing an *interface* method {2}, but the method''s host {3} is a *class*. This can lead to **IncompatibleClassChangeError** at runtime.".formatMessage(caller, instruction, methodReference, methodReference.hostClass)

}