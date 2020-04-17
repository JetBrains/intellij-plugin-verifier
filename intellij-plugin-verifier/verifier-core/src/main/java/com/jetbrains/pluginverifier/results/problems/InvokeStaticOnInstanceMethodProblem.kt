/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.results.problems

import com.jetbrains.plugin.structure.base.utils.formatMessage
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.reference.MethodReference
import java.util.*

class InvokeStaticOnInstanceMethodProblem(
  val methodReference: MethodReference,
  val resolvedMethod: MethodLocation,
  val caller: MethodLocation
) : CompatibilityProblem() {

  override val problemType
    get() = "Static method changed to instance method"

  override val shortDescription
    get() = "Attempt to execute *invokestatic* instruction on instance method {0}".formatMessage(resolvedMethod)

  override val fullDescription
    get() = ("Method {0} contains *invokestatic* instruction referencing instance method {1}, " +
      "what might have been caused by incompatible change of the method from static to instance. " +
      "This can lead to **IncompatibleClassChangeError** exception at runtime.").formatMessage(caller, resolvedMethod)

  override fun equals(other: Any?) = other is InvokeStaticOnInstanceMethodProblem
    && methodReference == other.methodReference
    && resolvedMethod == other.resolvedMethod
    && caller == other.caller

  override fun hashCode() = Objects.hash(methodReference, resolvedMethod, caller)
}