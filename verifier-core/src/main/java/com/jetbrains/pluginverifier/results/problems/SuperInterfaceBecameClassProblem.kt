package com.jetbrains.pluginverifier.results.problems

import com.jetbrains.pluginverifier.misc.formatMessage
import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.modifiers.Modifiers
import java.util.*

class SuperInterfaceBecameClassProblem(
    val child: ClassLocation,
    val clazz: ClassLocation
) : CompatibilityProblem() {

  override val problemType
    get() = "Incompatible change of super interface to class"

  override val shortDescription
    get() = "Incompatible change of super interface {0} to class".formatMessage(clazz)

  override val fullDescription: String
    get() {
      val type = if (child.modifiers.contains(Modifiers.Modifier.INTERFACE)) "Interface" else "Class"
      return "{0} {1} has a *super interface* {2} which is actually a *class*. This can lead to **IncompatibleClassChangeError** exception at runtime.".formatMessage(type, child, clazz)
    }

  override fun equals(other: Any?) = other is SuperInterfaceBecameClassProblem
      && child == other.child
      && clazz == other.clazz

  override fun hashCode() = Objects.hash(child, clazz)

}