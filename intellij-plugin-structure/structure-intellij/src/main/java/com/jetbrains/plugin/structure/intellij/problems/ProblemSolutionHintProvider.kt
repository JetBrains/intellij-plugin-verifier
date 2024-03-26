package com.jetbrains.plugin.structure.intellij.problems

import com.jetbrains.plugin.structure.base.problems.PluginProblem

fun interface ProblemSolutionHintProvider {
  fun getProblemSolutionHint(problem: PluginProblem): String?
}

object EmptyProblemSolutionHintProvider : ProblemSolutionHintProvider {
  override fun getProblemSolutionHint(problem: PluginProblem): String? = null
}