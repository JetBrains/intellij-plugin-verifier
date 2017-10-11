package com.jetbrains.pluginverifier.results.problems

import com.jetbrains.pluginverifier.misc.formatMessage
import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.presentation.PresentationUtils

data class MethodNotImplementedProblem(val method: MethodLocation,
                                       val incompleteClass: ClassLocation) : Problem() {

  override val shortDescription = "Abstract method {0} is not implemented".formatMessage(method)

  override val fullDescription = "Non-abstract class {0} inherits from {1} but doesn''t implement the abstract method {2}. This can lead to **AbstractMethodError** exception at runtime.".formatMessage(incompleteClass, method.hostClass, method.methodNameAndParameters(PresentationUtils.cutPackageConverter))
}