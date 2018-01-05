package com.jetbrains.pluginverifier.repository

class PluginIdAndVersion(pluginId: String,
                         version: String) : PluginInfo(pluginId, version) {
  override fun toString(): String = "$pluginId:$version"
}