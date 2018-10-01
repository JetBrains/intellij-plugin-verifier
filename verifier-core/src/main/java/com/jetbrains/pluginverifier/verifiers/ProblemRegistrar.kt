package com.jetbrains.pluginverifier.verifiers

import com.jetbrains.pluginverifier.results.deprecated.DeprecatedApiUsage
import com.jetbrains.pluginverifier.results.experimental.ExperimentalApiUsage
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem

interface ProblemRegistrar {
  fun registerProblem(problem: CompatibilityProblem)

  fun registerDeprecatedUsage(deprecatedApiUsage: DeprecatedApiUsage)

  fun registerExperimentalApiUsage(experimentalApiUsage: ExperimentalApiUsage)
}