package com.jetbrains.pluginverifier.results.problems

import com.jetbrains.pluginverifier.misc.formatMessage
import com.jetbrains.pluginverifier.results.location.ClassLocation
import java.util.*

class SuperClassBecameInterfaceProblem(
    val child: ClassLocation,
    val interfaze: ClassLocation
) : CompatibilityProblem() {

  override val shortDescription = "Incompatible change of super class {0} to interface".formatMessage(interfaze)

  override val fullDescription = "Class {0} has a *super class* {1} which is actually an *interface*. This can lead to **IncompatibleClassChangeError** at runtime.".formatMessage(child, interfaze)

  override fun equals(other: Any?) = other is SuperClassBecameInterfaceProblem
      && child == other.child
      && interfaze == other.interfaze

  override fun hashCode() = Objects.hash(child, interfaze)

}