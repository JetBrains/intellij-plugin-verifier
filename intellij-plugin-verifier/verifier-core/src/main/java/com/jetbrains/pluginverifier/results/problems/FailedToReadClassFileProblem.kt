package com.jetbrains.pluginverifier.results.problems

import com.jetbrains.plugin.structure.base.utils.formatMessage
import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.reference.ClassReference
import java.util.*

class FailedToReadClassFileProblem(
  val failedClass: ClassReference,
  val usage: Location,
  val reason: String
) : CompatibilityProblem() {

  override val problemType
    get() = "Failed to read class"

  override val shortDescription
    get() = "Failed to read class {0}".formatMessage(failedClass)

  override val fullDescription
    get() = ("Class {0} referenced in {1} cannot be read. " +
      "Invalid classes can lead to **ClassFormatError** exception at runtime.").formatMessage(failedClass, usage, reason)

  override fun equals(other: Any?) = other is FailedToReadClassFileProblem
    && failedClass == other.failedClass
    && usage == other.usage

  override fun hashCode() = Objects.hash(failedClass, usage)

}