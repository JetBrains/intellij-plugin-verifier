/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.results.problems

import com.jetbrains.plugin.structure.base.utils.formatMessage
import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.location.MethodLocation
import java.util.*

class AbstractClassInstantiationProblem(
  val abstractClass: ClassLocation,
  val creator: MethodLocation
) : CompatibilityProblem() {

  override val problemType
    get() = "Instantiation of an abstract class"

  override val shortDescription
    get() = "Instantiation of an abstract class {0}".formatMessage(abstractClass)

  override val fullDescription
    get() = "Method {0} has instantiation *new* instruction referencing an abstract class {1}. This can lead to **InstantiationError** exception at runtime.".formatMessage(creator, abstractClass)

  override fun equals(other: Any?) = other is AbstractClassInstantiationProblem
    && abstractClass == other.abstractClass
    && creator == other.creator

  override fun hashCode() = Objects.hash(abstractClass, creator)

}