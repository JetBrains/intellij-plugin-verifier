package com.jetbrains.pluginverifier.results.problems

import com.jetbrains.plugin.structure.base.utils.formatMessage
import com.jetbrains.pluginverifier.results.location.MethodLocation
import java.util.*

class InvokeStaticOnInstanceMethodProblem(
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
      && resolvedMethod == other.resolvedMethod
      && caller == other.caller

  override fun hashCode() = Objects.hash(resolvedMethod, caller)
}