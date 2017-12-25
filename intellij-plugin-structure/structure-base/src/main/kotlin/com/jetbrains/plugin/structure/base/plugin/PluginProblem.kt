package com.jetbrains.plugin.structure.base.plugin

import java.util.*

abstract class PluginProblem {

  abstract val level: Level

  abstract val message: String

  enum class Level {
    ERROR,
    WARNING
  }

  final override fun toString(): String = message

  final override fun equals(other: Any?) = other is PluginProblem
      && level == other.level && message == other.message

  final override fun hashCode() = Objects.hash(message, level)

}