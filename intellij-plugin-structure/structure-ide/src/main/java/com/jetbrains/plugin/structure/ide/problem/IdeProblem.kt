package com.jetbrains.plugin.structure.ide.problem

abstract class IdeProblem {
  abstract val level: Level

  abstract val message: String

  enum class Level {
    ERROR,
    WARNING
  }
}