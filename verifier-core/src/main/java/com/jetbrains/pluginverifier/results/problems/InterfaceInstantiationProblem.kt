package com.jetbrains.pluginverifier.results.problems

import com.jetbrains.pluginverifier.misc.formatMessage
import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.location.MethodLocation

data class InterfaceInstantiationProblem(val interfaze: ClassLocation,
                                         val creator: MethodLocation) : Problem() {

  override val shortDescription = "Instantiation of an interface {0}".formatMessage(interfaze)

  override val fullDescription = "Method {0} has instantiation *new* instruction referencing an interface {1}. This can lead to **InstantiationError** exception at runtime.".formatMessage(creator, interfaze)

}