package com.jetbrains.pluginverifier.results.problems

import com.jetbrains.pluginverifier.misc.formatMessage
import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.modifiers.Modifiers
import java.util.*

class InheritFromFinalClassProblem(
    val child: ClassLocation,
    val finalClass: ClassLocation
) : CompatibilityProblem() {

  override val shortDescription = "Inheritance from a final class {0}".formatMessage(finalClass)

  override val fullDescription: String
    get() {
      val type = if (child.modifiers.contains(Modifiers.Modifier.INTERFACE)) "Interface" else "Class"
      return "{0} {1} inherits from a final class {2}. This can lead to **VerifyError** exception at runtime.".formatMessage(type, child, finalClass)
    }

  override fun equals(other: Any?) = other is InheritFromFinalClassProblem
      && finalClass == other.finalClass
      && child == other.child

  override fun hashCode() = Objects.hash(finalClass, child)
}