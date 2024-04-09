package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.base.problems.PluginProblem

interface StructurallyValidated {
  val problems: List<PluginProblem>
}