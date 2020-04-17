/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.results.problems

import com.jetbrains.plugin.structure.base.utils.formatMessage
import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.reference.ClassReference
import java.util.*

/**
 * Indicates that a class, which is used in [usage], couldn't be read.
 */
class InvalidClassFileProblem(
  val invalidClass: ClassReference,
  val usage: Location,
  val message: String
) : CompatibilityProblem() {

  override val problemType
    get() = "Invalid class file"

  override val shortDescription
    get() = "Invalid class-file {0}".formatMessage(invalidClass)

  override val fullDescription
    get() = ("Class {0} referenced in {1} cannot be read: {2}. You may try to recompile the class-file. " +
      "Invalid classes can lead to **ClassFormatError** exception at runtime.").formatMessage(invalidClass, usage, message)

  override fun equals(other: Any?) = other is InvalidClassFileProblem
    && invalidClass == other.invalidClass
    && usage == other.usage

  override fun hashCode() = Objects.hash(invalidClass, usage)

}