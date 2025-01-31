/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.results.problems

import com.jetbrains.plugin.structure.base.utils.formatMessage
import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.reference.ClassReference
import java.util.*

class ClassNotFoundProblem(
  val unresolved: ClassReference,
  val usage: Location
) : CompatibilityProblem() {

  override val problemType
    get() = "Class not found"

  override val shortDescription
    get() = "Access to unresolved class {0}".formatMessage(unresolved)

  override val fullDescription: String
    get() {
      val elementType = usage.elementType.presentableName.capitalize()
      return "{0} {1} references an unresolved class {2}. This can lead to **NoSuchClassError** exception at runtime."
        .formatMessage(elementType, usage, unresolved)
    }

  override fun equals(other: Any?) = other is ClassNotFoundProblem
    && unresolved == other.unresolved
    && usage == other.usage

  override fun hashCode() = Objects.hash(unresolved, usage)

  override val isCritical = true
}