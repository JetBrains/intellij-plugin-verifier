/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.results.problems

import com.jetbrains.plugin.structure.base.utils.formatMessage
import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.modifiers.Modifiers
import java.util.*

class InheritFromFinalClassProblem(
  val child: ClassLocation,
  val finalClass: ClassLocation
) : CompatibilityProblem() {

  override val problemType
    get() = "Final class inheritance"

  override val shortDescription
    get() = "Inheritance from a final class {0}".formatMessage(finalClass)

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