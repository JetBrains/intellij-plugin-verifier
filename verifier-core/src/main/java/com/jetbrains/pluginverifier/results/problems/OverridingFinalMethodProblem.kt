package com.jetbrains.pluginverifier.results.problems

import com.jetbrains.pluginverifier.misc.formatMessage
import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.location.MethodLocation

data class OverridingFinalMethodProblem(val method: MethodLocation,
                                        val invalidClass: ClassLocation) : Problem() {

  override val shortDescription = "Overriding a final method {0}".formatMessage(method)

  override val fullDescription = "Class {0} overrides the final method {1}. This can lead to **VerifyError** exception at runtime.".formatMessage(invalidClass, method)
}