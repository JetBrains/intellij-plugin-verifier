package com.intellij.structure.problems

abstract class PluginProblem {

  abstract val level: Level

  abstract val message: String

  enum class Level {
    ERROR,
    WARNING
  }

  final override fun toString(): String = message

}