package com.jetbrains.pluginverifier.results.structure

/**
 * Represents a minor problem of the plugin's structure,
 * such as missing description or change notes.
 *
 * This class is a mirror of the [warning] [com.jetbrains.plugin.structure.base.plugin.PluginProblem.Level.WARNING]
 * from the _intellij-plugin-structure_ module.
 */
data class PluginStructureWarning(val message: String) {
  override fun toString() = message
}