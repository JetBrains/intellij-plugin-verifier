package com.jetbrains.pluginverifier.results.problems

import com.jetbrains.pluginverifier.misc.formatMessage
import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.reference.ClassReference

data class InvalidClassFileProblem(val invalidClass: ClassReference,
                                   val usage: Location,
                                   val asmError: String) : CompatibilityProblem() {

  override val shortDescription = "Invalid class-file {0}".formatMessage(invalidClass)

  override val fullDescription = ("Class {0} referenced from {1} cannot be read using the ASM Java Bytecode engineering library. " +
      "The internal ASM exception: {2}. You may try to recompile the class-file. Invalid classes can lead to **ClassFormatError** exception at runtime.").formatMessage(invalidClass, usage, asmError)

}