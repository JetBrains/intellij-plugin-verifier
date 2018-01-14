package com.jetbrains.pluginverifier.results.warnings

data class Warning(val message: String) {
  override fun toString(): String = message
}