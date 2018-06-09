package com.jetbrains.pluginverifier.results.problems

import com.jetbrains.pluginverifier.misc.formatMessage
import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.reference.ClassReference
import java.util.*

class InvalidClassFileProblem(
    val invalidClass: ClassReference,
    val usage: Location,
    val asmError: String
) : CompatibilityProblem() {

  override val problemType
    get() = "Invalid class file"

  override val shortDescription
    get() = "Invalid class-file {0}".formatMessage(invalidClass)

  override val fullDescription
    get() = ("Class {0} referenced in {1} cannot be read using the ASM Java Bytecode engineering library. " +
        "The internal ASM exception: {2}. You may try to recompile the class-file. Invalid classes can lead to **ClassFormatError** exception at runtime.").formatMessage(invalidClass, usage, asmError)

  override fun equals(other: Any?) = other is InvalidClassFileProblem
      && invalidClass == other.invalidClass
      && usage == other.usage

  override fun hashCode() = Objects.hash(invalidClass, usage)

}