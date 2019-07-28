package com.jetbrains.pluginverifier.warnings

import com.jetbrains.pluginverifier.dependencies.DependencyNode

data class DependenciesCycleWarning(val cycle: List<DependencyNode>) : CompatibilityWarning() {
  override val message
    get() = "The plugin is on a dependencies cycle: " + cycle.joinToString(separator = " -> ") + " -> " + cycle[0]
}