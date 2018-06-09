package com.jetbrains.pluginverifier.results.problems

import com.jetbrains.pluginverifier.misc.formatMessage
import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.location.FieldLocation
import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.presentation.methodOrConstructorWord
import com.jetbrains.pluginverifier.results.reference.ClassReference
import java.util.*

class ClassNotFoundProblem(
    val unresolved: ClassReference,
    val usage: Location
) : CompatibilityProblem() {

  override val problemType
    get() = "Missing class access"

  override val shortDescription
    get() = "Access to unresolved class {0}".formatMessage(unresolved)

  override val fullDescription: String
    get() {
      val type = when (usage) {
        is ClassLocation -> "Class"
        is MethodLocation -> usage.methodOrConstructorWord.capitalize()
        is FieldLocation -> "Field"
      }
      return "{0} {1} references an unresolved class {2}. This can lead to **NoSuchClassError** exception at runtime.".formatMessage(type, usage, unresolved)
    }

  override fun equals(other: Any?) = other is ClassNotFoundProblem
      && unresolved == other.unresolved
      && usage == other.usage

  override fun hashCode() = Objects.hash(unresolved, usage)
}