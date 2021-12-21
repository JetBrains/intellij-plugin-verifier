package com.jetbrains.plugin.structure.edu.problems

import com.jetbrains.plugin.structure.base.plugin.PluginProblem

// Important: Language id here should be the same as in Language.getID() from IntelliJ platform
enum class Language(val id: String) {
  OBJECTIVE_C("ObjectiveC"),
  GO("go"),
  JAVA("JAVA"),
  JAVA_SCRIPT("JavaScript"),
  KOTLIN("kotlin"),
  PYTHON("Python"),
  RUST("Rust"),
  SCALA("Scala"),
  PHP("PHP")
}

object UnsupportedProgrammingLanguage : PluginProblem() {
  override val level = Level.ERROR
  override val message = "Only ${Language.values().joinToString { it.id }} languages are supported"
}