package com.jetbrains.pluginverifier.warnings

abstract class CompatibilityWarning {

  abstract val shortDescription: String

  abstract val fullDescription: String

  final override fun toString(): String = fullDescription
}