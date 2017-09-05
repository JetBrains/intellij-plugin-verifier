package com.jetbrains.pluginverifier.api

import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.pluginverifier.problems.Problem

interface ProblemsFilter {
  fun isRelevantProblem(plugin: IdePlugin, problem: Problem): Boolean

  object AlwaysTrue : ProblemsFilter {
    override fun isRelevantProblem(plugin: IdePlugin, problem: Problem): Boolean = true
  }
}