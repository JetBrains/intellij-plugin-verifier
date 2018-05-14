package com.jetbrains.pluginverifier.results.problems

import com.jetbrains.pluginverifier.misc.formatMessage
import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.reference.ClassReference
import java.util.*

class FailedToReadClassFileProblem(
    val failedClass: ClassReference,
    val usage: Location,
    val reason: String
) : CompatibilityProblem() {

  override val shortDescription = "Failed to read class {0}".formatMessage(failedClass)

  override val fullDescription = ("Class {0} referenced in {1} cannot be read: {2}. Unavailable classes can lead to " +
      "**NoSuchClassError** or **ClassFormatError** exceptions at runtime.").formatMessage(failedClass, usage, reason)

  override fun equals(other: Any?) = other is FailedToReadClassFileProblem
      && failedClass == other.failedClass
      && usage == other.usage

  override fun hashCode() = Objects.hash(failedClass, usage)

}