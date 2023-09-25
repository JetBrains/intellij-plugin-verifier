package com.jetbrains.plugin.structure.base.problems

import com.jetbrains.plugin.structure.base.plugin.PluginProblem

class ReclassifiedPluginProblem(private val reclassifiedLevel: Level, private val originalProblem: PluginProblem) : PluginProblem() {
  override val level: Level
    get() = reclassifiedLevel
  override val message: String
    get() = originalProblem.message
  val unwrapped: PluginProblem
    get() = originalProblem
}