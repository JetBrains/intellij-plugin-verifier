package com.jetbrains.pluginverifier.results.warnings

/**
 * @author Sergey Patrikeev
 */
data class Warning(val message: String) {
  override fun toString(): String = message
}