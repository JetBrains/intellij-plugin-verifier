package com.jetbrains.plugin.structure.edu.problems

import com.jetbrains.plugin.structure.base.problems.PluginProblem

class InvalidVersionError(version: String) : PluginProblem() {
  override val level = Level.ERROR
  override val message = "Broken Edu plugin version $version."
}