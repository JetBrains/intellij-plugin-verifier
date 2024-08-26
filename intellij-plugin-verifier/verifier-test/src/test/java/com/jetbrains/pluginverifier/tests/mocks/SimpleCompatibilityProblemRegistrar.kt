package com.jetbrains.pluginverifier.tests.mocks

import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.verifiers.ProblemRegistrar
import com.jetbrains.pluginverifier.warnings.CompatibilityWarning
import com.jetbrains.pluginverifier.warnings.WarningRegistrar

class SimpleCompatibilityProblemRegistrar : ProblemRegistrar, WarningRegistrar {
  private val _problems = mutableListOf<CompatibilityProblem>()
  val problems: List<CompatibilityProblem>
    get() = _problems

  private val _warnings = mutableListOf<CompatibilityWarning>()
  val warnings: List<CompatibilityWarning>
    get() = _warnings

  override fun registerProblem(problem: CompatibilityProblem) {
    _problems += problem
  }

  override fun registerCompatibilityWarning(warning: CompatibilityWarning) {
    _warnings += warning
  }
}