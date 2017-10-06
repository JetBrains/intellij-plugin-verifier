package com.jetbrains.pluginverifier.dependencies.graph

import com.jetbrains.pluginverifier.plugin.PluginDetails

data class DepVertex(val dependencyId: String, val pluginDetails: PluginDetails) {
  override fun equals(other: Any?): Boolean = other is DepVertex && dependencyId == other.dependencyId

  override fun hashCode(): Int = dependencyId.hashCode()
}