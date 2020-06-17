package com.jetbrains.plugin.structure.edu.problems

import com.jetbrains.plugin.structure.base.plugin.PluginProblem

object UnsupportedProgrammingLanguage : PluginProblem() {
  // Important: Language name here should be the same as in Language.getID()
  val supportedLanguages = listOf("ObjectiveC", "go", "JAVA", "JavaScript", "kotlin", "Python", "Rust", "Scala")

  override val level = Level.ERROR
  override val message = "Only ${supportedLanguages.joinToString()} languages are supported"
}