/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.results.problems

import com.jetbrains.plugin.structure.base.utils.formatMessage
import com.jetbrains.pluginverifier.results.instruction.Instruction
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.reference.ClassReference
import com.jetbrains.pluginverifier.results.reference.MethodReference
import java.util.*

class InvokeClassMethodOnInterfaceProblem(
  val methodReference: MethodReference,
  val caller: MethodLocation,
  val instruction: Instruction
) : CompatibilityProblem() {

  override val problemType
    get() = "Incompatible change of class to interface"

  val changedClass: ClassReference
    get() = methodReference.hostClass

  override val shortDescription
    get() = "Incompatible change of class {0} to interface".formatMessage(changedClass)

  override val fullDescription
    get() = "Method {0} has invocation *{1}* instruction referencing a *class* method {2}, but the method''s host {3} is an *interface*. This can lead to **IncompatibleClassChangeError** at runtime.".formatMessage(caller, instruction, methodReference, methodReference.hostClass)

  override fun equals(other: Any?) = other is InvokeClassMethodOnInterfaceProblem
    && methodReference == other.methodReference
    && caller == other.caller
    && instruction == other.instruction

  override fun hashCode() = Objects.hash(methodReference, caller, instruction)

}



