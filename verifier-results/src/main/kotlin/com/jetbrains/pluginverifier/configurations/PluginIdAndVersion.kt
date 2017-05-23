package com.jetbrains.pluginverifier.configurations

data class PluginIdAndVersion(val pluginId: String, val version: String) {
  override fun toString(): String = "$pluginId:$version"
}