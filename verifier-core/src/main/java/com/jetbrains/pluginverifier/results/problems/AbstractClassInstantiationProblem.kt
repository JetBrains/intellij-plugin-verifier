package com.jetbrains.pluginverifier.results.problems

import com.jetbrains.pluginverifier.misc.formatMessage
import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.location.MethodLocation

data class AbstractClassInstantiationProblem(val abstractClass: ClassLocation,
                                             val creator: MethodLocation) : CompatibilityProblem() {

  override val shortDescription = "Instantiation of an abstract class {0}".formatMessage(abstractClass)

  override val fullDescription: String = "Method {0} has instantiation *new* instruction referencing an abstract class {1}. This can lead to **InstantiationError** exception at runtime.".formatMessage(creator, abstractClass)

}