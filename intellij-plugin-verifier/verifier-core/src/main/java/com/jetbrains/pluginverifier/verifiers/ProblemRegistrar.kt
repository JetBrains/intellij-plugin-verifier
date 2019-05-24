package com.jetbrains.pluginverifier.verifiers

import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem

interface ProblemRegistrar {
  val allProblems: Set<CompatibilityProblem>

  fun registerProblem(problem: CompatibilityProblem)

  fun unregisterProblem(problem: CompatibilityProblem)
}