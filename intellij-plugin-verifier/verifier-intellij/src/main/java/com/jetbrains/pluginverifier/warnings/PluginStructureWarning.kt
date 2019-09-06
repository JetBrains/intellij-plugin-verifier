package com.jetbrains.pluginverifier.warnings

import com.jetbrains.plugin.structure.base.plugin.PluginProblem

data class PluginStructureWarning(private val pluginProblem: PluginProblem) {

  init {
    check(pluginProblem.level == PluginProblem.Level.WARNING)
  }

  val message: String get() = pluginProblem.message
}