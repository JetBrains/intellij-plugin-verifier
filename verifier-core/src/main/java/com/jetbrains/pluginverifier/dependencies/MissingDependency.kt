package com.jetbrains.pluginverifier.dependencies

import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import java.io.Serializable

/**
 * Represents a [dependency] of the [verified plugin] [DependenciesGraph.verifiedPlugin]
 * that was not resolved due to [missingReason].
 */
data class MissingDependency(
    val dependency: PluginDependency,
    val missingReason: String
) : Serializable {
  override fun toString() = "$dependency: $missingReason"

  companion object {
    private const val serialVersionUID = 0L
  }
}