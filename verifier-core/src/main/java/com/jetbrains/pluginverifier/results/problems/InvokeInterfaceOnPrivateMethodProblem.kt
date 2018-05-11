package com.jetbrains.pluginverifier.results.problems

import com.jetbrains.pluginverifier.misc.formatMessage
import com.jetbrains.pluginverifier.results.location.MethodLocation
import java.util.*

class InvokeInterfaceOnPrivateMethodProblem(
    val resolvedMethod: MethodLocation,
    val caller: MethodLocation
) : CompatibilityProblem() {

  override val shortDescription = "Attempt to execute an *invokeinterface* instruction on a private method {0}".formatMessage(resolvedMethod)

  override val fullDescription = "Method {0} contains an *invokeinterface* instruction referencing a private method {1}. This can lead to **IncompatibleClassChangeError** exception at runtime.".formatMessage(caller, resolvedMethod)

  override fun equals(other: Any?) = other is InvokeInterfaceOnPrivateMethodProblem
      && resolvedMethod == other.resolvedMethod
      && caller == other.caller

  override fun hashCode() = Objects.hash(resolvedMethod, caller)

}