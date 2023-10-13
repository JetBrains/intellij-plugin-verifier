/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.base.problems

import java.util.*

abstract class PluginProblem {

  abstract val level: Level

  abstract val message: String
  open val hint: ProblemSolutionHint? = null

  enum class Level {
    ERROR,
    WARNING,
    UNACCEPTABLE_WARNING
  }

  final override fun toString() = message

  final override fun equals(other: Any?) = other is PluginProblem
    && level == other.level && message == other.message

  final override fun hashCode() = Objects.hash(message, level)
}

/**
 * Indicates a hint that points to the solution.
 * @param example A code sample that shows a correct usage of the specific code or declaration.
 * @param documentationUrl a hyperlink to the human-readable documentation describing a suggested usage.
 */
data class ProblemSolutionHint(val example: String? = null, val documentationUrl: String? = null)