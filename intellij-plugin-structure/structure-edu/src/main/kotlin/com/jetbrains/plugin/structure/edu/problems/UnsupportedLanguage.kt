package com.jetbrains.plugin.structure.edu.problems

import com.jetbrains.plugin.structure.base.plugin.PluginProblem

object UnsupportedLanguage : PluginProblem() {
  override val level
    get() = Level.ERROR

  override val message
    get() = "Unknown language"

}