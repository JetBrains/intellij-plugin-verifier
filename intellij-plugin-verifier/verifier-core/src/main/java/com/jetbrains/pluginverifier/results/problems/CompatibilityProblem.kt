/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.results.problems

/**
 * Base class for all compatibility problems.
 *
 * Each problem has a [short] [shortDescription] description
 * which can be used to group similar problems of several plugins,
 * and a [full] [fullDescription] description containing all the details,
 * such as problem's location in bytecode, JVM specification
 * reference and possible effect.
 *
 * Each problem has a certain [problemType],
 * which can be used to group similar problems.
 */
abstract class CompatibilityProblem {

  abstract val problemType: String

  abstract val shortDescription: String

  abstract val fullDescription: String

  abstract override fun equals(other: Any?): Boolean

  abstract override fun hashCode(): Int

  final override fun toString() = fullDescription

  open val isCritical: Boolean = false

}