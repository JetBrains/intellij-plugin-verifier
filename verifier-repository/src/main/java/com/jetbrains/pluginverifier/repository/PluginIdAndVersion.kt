package com.jetbrains.pluginverifier.repository

data class PluginIdAndVersion(override val pluginId: String,
                              override val version: String) : PluginInfo {
  override fun toString(): String = "$pluginId:$version"
}