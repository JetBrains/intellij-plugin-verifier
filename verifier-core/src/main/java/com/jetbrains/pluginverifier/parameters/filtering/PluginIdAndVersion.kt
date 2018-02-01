package com.jetbrains.pluginverifier.parameters.filtering

import com.jetbrains.pluginverifier.repository.PluginInfo

data class PluginIdAndVersion(val pluginId: String, val version: String) {
  override fun toString() = "$pluginId $version"
}

fun PluginInfo.toPluginIdAndVersion() = PluginIdAndVersion(pluginId, version)