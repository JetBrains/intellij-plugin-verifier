package com.jetbrains.pluginverifier.results.problems

import com.jetbrains.pluginverifier.misc.formatMessage
import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.location.FieldLocation
import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.reference.ClassReference

data class ClassNotFoundProblem(val unresolved: ClassReference,
                                val usage: Location) : Problem() {

  override val shortDescription = "Access to unresolved class {0}".formatMessage(unresolved)

  override val fullDescription: String
    get() {
      val type = when (usage) {
        is ClassLocation -> "Class"
        is MethodLocation -> "Method"
        is FieldLocation -> "Field"
        else -> throw IllegalArgumentException()
      }
      return "{0} {1} references an unresolved class {2}. This can lead to **NoSuchClassError** exception at runtime.".formatMessage(type, usage, unresolved)
    }
}