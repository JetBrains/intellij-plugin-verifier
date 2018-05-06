package com.jetbrains.pluginverifier.parameters.filtering

import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.pluginverifier.repository.PluginInfo

data class PluginIdAndVersion(val pluginId: String, val version: String) {
  override fun toString() = "$pluginId $version"
}

fun IdePlugin.toPluginIdAndVersion() = PluginIdAndVersion(pluginId!!, pluginVersion!!)

fun PluginInfo.toPluginIdAndVersion() = PluginIdAndVersion(pluginId, version)