package com.jetbrains.pluginverifier.results.problems

import com.jetbrains.pluginverifier.misc.formatMessage
import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.reference.ClassReference

data class InvalidClassFileProblem(val brokenClass: ClassReference,
                                   val usage: Location,
                                   val reason: String) : Problem() {

  override val shortDescription = "Invalid class-file {0}".formatMessage(brokenClass)

  override val fullDescription = "Class-file {0} referenced from {1} is invalid: {2}. This can lead to **ClassFormatError** exception at runtime.".formatMessage(brokenClass, usage, reason)

}