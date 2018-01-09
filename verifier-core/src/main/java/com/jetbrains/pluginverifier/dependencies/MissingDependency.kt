package com.jetbrains.pluginverifier.dependencies

import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency

//todo: make missingReason a separate class with more information.
/**
 * Represents a [dependency] that was not found on the dependency
 * resolution phase due to [missingReason].
 */
data class MissingDependency(val dependency: PluginDependency,
                             val missingReason: String) {
  override fun toString(): String = "$dependency: $missingReason"
}