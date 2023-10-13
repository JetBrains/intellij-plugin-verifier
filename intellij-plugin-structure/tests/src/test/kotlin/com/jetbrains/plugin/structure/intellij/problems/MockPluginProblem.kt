package com.jetbrains.plugin.structure.intellij.problems

import com.jetbrains.plugin.structure.base.problems.PluginProblem

class MockPluginProblem : PluginProblem() {
  override val level: Level
    get() = Level.ERROR
  override val message: String
    get() = "Mock unclassified fatal plugin error"
}
