package com.jetbrains.pluginverifier.results

/**
 * Represents a fatal plugin's structure error,
 * such as missing mandatory field in the plugin descriptor (`<id>`, `<version>`, etc.).
 *
 * This class is a mirror of the [error] [com.jetbrains.plugin.structure.base.plugin.PluginProblem.Level.ERROR]
 * from the _intellij-plugin-structure_ module.
 */
data class PluginStructureError(val message: String) {
  override fun toString() = message
}