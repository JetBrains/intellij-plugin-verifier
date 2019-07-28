package com.jetbrains.pluginverifier.warnings

import com.jetbrains.plugin.structure.base.plugin.PluginProblem

data class PluginStructureWarning(val pluginProblem: PluginProblem) : CompatibilityWarning() {

  init {
    check(pluginProblem.level == PluginProblem.Level.WARNING)
  }

  override val message = pluginProblem.message
}