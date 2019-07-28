package com.jetbrains.pluginverifier.warnings

abstract class CompatibilityWarning {
  abstract val message: String

  final override fun toString(): String = message
}