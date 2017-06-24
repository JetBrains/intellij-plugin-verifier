package com.jetbrains.pluginverifier.tasks

data class PluginIdAndVersion(val pluginId: String, val version: String) {
  override fun toString(): String = "$pluginId:$version"
}