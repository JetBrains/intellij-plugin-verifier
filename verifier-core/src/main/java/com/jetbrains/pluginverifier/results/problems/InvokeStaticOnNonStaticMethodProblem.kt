package com.jetbrains.pluginverifier.results.problems

import com.jetbrains.pluginverifier.misc.formatMessage
import com.jetbrains.pluginverifier.results.location.MethodLocation

data class InvokeStaticOnNonStaticMethodProblem(val resolvedMethod: MethodLocation,
                                                val caller: MethodLocation) : Problem() {

  override val shortDescription = "Attempt to execute an *invokestatic* instruction on a non-static method {0}".formatMessage(resolvedMethod)

  override val fullDescription = "Method {0} contains an *invokestatic* instruction referencing a non-static method {1}. This can lead to **IncompatibleClassChangeError** exception at runtime.".formatMessage(caller, resolvedMethod)
}