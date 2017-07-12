package com.jetbrains.structure.plugin

abstract class PluginProblem {

  abstract val level: Level

  abstract val message: String

  enum class Level {
    ERROR,
    WARNING
  }

  final override fun toString(): String = message

}