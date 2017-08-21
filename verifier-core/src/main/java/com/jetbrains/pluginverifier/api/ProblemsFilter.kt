package com.jetbrains.pluginverifier.api

import com.intellij.structure.plugin.Plugin
import com.jetbrains.pluginverifier.problems.Problem

interface ProblemsFilter {
  fun isRelevantProblem(plugin: Plugin, problem: Problem): Boolean

  object AlwaysTrue : ProblemsFilter {
    override fun isRelevantProblem(plugin: Plugin, problem: Problem): Boolean = true
  }
}