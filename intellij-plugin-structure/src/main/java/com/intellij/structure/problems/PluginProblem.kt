package com.intellij.structure.problems

interface PluginProblem {

  val level: Level

  val message: String

  enum class Level {
    ERROR,
    WARNING
  }

}