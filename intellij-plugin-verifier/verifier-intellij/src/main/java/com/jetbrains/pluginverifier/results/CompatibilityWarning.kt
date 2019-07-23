package com.jetbrains.pluginverifier.results

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.pluginverifier.dependencies.DependencyNode

abstract class CompatibilityWarning {
  abstract val message: String

  final override fun toString(): String = message
}

data class PluginStructureWarning(val pluginProblem: PluginProblem) : CompatibilityWarning() {

  init {
    check(pluginProblem.level == PluginProblem.Level.WARNING)
  }

  override val message = pluginProblem.message
}

data class DependenciesCycleWarning(val cycle: List<DependencyNode>) : CompatibilityWarning() {
  override val message
    get() = "The plugin is on a dependencies cycle: " + cycle.joinToString(separator = " -> ") + " -> " + cycle[0]
}

data class MistakenlyBundledIdePackagesWarning(private val idePackages: List<String>) : CompatibilityWarning() {
  override val message = buildString {
    append("The plugin distribution contains IDE packages: ")
    if (idePackages.size < 5) {
      append(idePackages.joinToString())
    } else {
      append(idePackages.take(3).joinToString())
      append(" and ${idePackages.size - 3} other")
    }
    append(". ")
    append("Bundling IDE classes is considered bad practice and may lead to sophisticated compatibility problems. ")
    append("Consider excluding IDE classes from the plugin distribution and reusing the IDE's classes. ")
    append("If your plugin depends on classes of an IDE bundled plugin, explicitly specify dependency on that plugin instead of bundling it. ")
  }
}

