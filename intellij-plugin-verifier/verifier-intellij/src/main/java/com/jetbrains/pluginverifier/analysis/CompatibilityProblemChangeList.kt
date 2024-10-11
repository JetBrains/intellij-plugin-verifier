/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.analysis

import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem

class CompatibilityProblemChangeList {

  private val _addedProblems = hashSetOf<CompatibilityProblem>()

  val addedProblems: Set<CompatibilityProblem>
    get() = _addedProblems

  private val _removedProblems = hashSetOf<CompatibilityProblem>()

  val removedProblems: Set<CompatibilityProblem>
    get() = _removedProblems

  operator fun plusAssign(problem: CompatibilityProblem) {
    _addedProblems += problem
  }

  operator fun minusAssign(problem: CompatibilityProblem) {
    _removedProblems += problem
  }

  fun first(): CompatibilityProblem = problems.first()

  val problems: Set<CompatibilityProblem>
    get() = addedProblems - removedProblems

  val size: Int
    get() = problems.size
}