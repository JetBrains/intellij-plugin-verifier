package com.jetbrains.pluginverifier.warnings

import com.jetbrains.pluginverifier.dependencies.DependencyNode

data class DependenciesCycleWarning(val cycle: List<DependencyNode>) : CompatibilityWarning() {

  override val shortDescription
    get() = "Plugin dependencies are cyclic"

  override val fullDescription
    get() = "The plugin is on a dependencies cycle: " + cycle.joinToString(separator = " -> ") + " -> " + cycle[0]
}