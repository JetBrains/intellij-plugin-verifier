/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.results.problems

import com.jetbrains.plugin.structure.base.utils.formatMessage
import com.jetbrains.pluginverifier.results.location.ClassLocation
import java.util.*

class SuperClassBecameInterfaceProblem(
  val child: ClassLocation,
  val interfaze: ClassLocation
) : CompatibilityProblem() {

  override val problemType
    get() = "Incompatible change of super class to interface"

  override val shortDescription
    get() = "Incompatible change of super class {0} to interface".formatMessage(interfaze)

  override val fullDescription
    get() = "Class {0} has a *super class* {1} which is actually an *interface*. This can lead to **IncompatibleClassChangeError** at runtime.".formatMessage(child, interfaze)

  override fun equals(other: Any?) = other is SuperClassBecameInterfaceProblem
    && child == other.child
    && interfaze == other.interfaze

  override fun hashCode() = Objects.hash(child, interfaze)

}