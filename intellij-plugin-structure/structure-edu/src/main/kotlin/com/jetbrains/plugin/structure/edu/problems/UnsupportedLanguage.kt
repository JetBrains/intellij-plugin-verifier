package com.jetbrains.plugin.structure.edu.problems

import com.jetbrains.plugin.structure.base.problems.PluginProblem

class UnsupportedLanguage(language: String?) : PluginProblem() {
  override val level = Level.ERROR
  override val message = "The language $language is not supported."
}