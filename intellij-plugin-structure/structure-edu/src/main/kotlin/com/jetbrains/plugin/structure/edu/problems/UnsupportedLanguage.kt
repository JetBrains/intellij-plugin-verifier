package com.jetbrains.plugin.structure.edu.problems

import com.jetbrains.plugin.structure.base.plugin.PluginProblem

class UnsupportedLanguage(language: String?) : PluginProblem() {
  override val level = Level.ERROR
  override val message = "Unknown language $language "
}