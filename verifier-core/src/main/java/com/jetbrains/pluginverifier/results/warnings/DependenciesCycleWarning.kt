package com.jetbrains.pluginverifier.results.warnings

import com.jetbrains.plugin.structure.base.plugin.PluginProblem

data class DependenciesCycleWarning(val cyclePresentation: String) : PluginProblem() {
  override val level = Level.WARNING

  override val message: String = "The plugin is on a dependencies cycle: $cyclePresentation"
}