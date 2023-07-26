package com.jetbrains.plugin.structure.intellij.problems

import com.jetbrains.plugin.structure.base.plugin.PluginProblem

class BlocklistedPluginError(val cause: PluginProblem) : PluginProblem() {
  override val level: Level
    get() = Level.ERROR
  override val message: String
    get() = "Fatal plugin problem has been detected. This problem is not registered in the list of supported of fatal plugin errors. " +
      "Please contact developers. Error: ${cause.javaClass}, message: ${cause.message}"
}