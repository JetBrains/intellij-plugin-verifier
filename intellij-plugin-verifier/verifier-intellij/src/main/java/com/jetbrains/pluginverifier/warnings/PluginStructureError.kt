package com.jetbrains.pluginverifier.warnings

import com.jetbrains.plugin.structure.base.plugin.PluginProblem

/**
 * Represents a fatal plugin's structure error,
 * such as missing mandatory field in the plugin descriptor (`<id>`, `<version>`, etc.).
 */
data class PluginStructureError(private val pluginProblem: PluginProblem) {
  init {
    check(pluginProblem.level == PluginProblem.Level.ERROR)
  }

  val message: String
    get() = pluginProblem.message

  override fun toString() = message
}