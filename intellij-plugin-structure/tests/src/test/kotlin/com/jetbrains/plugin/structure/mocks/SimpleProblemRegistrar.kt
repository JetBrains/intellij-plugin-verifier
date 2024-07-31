package com.jetbrains.plugin.structure.mocks

import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.intellij.verifiers.ProblemRegistrar

class SimpleProblemRegistrar : ProblemRegistrar {
  private val _problems = mutableListOf<PluginProblem>()

  val problems: List<PluginProblem>
    get() = _problems

  override fun registerProblem(problem: PluginProblem) {
    _problems += problem
  }

  fun reset() {
    _problems.clear()
  }
}