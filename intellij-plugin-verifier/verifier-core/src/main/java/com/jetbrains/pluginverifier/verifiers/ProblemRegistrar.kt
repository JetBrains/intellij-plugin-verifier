package com.jetbrains.pluginverifier.verifiers

import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem

interface ProblemRegistrar {

  fun registerProblem(problem: CompatibilityProblem)

}