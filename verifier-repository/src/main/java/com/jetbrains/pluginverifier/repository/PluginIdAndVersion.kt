package com.jetbrains.pluginverifier.repository

import java.util.*

/**
 * [PluginInfo] having only plugin ID and version specified.
 */
class PluginIdAndVersion(
    pluginId: String,
    version: String
) : PluginInfo(pluginId, pluginId, version, null, null, null) {

  override val presentableName
    get() = "$pluginId $version"

  override fun equals(other: Any?) = other is PluginIdAndVersion
      && pluginId == other.pluginId
      && version == other.version

  override fun hashCode() = Objects.hash(pluginId, version)

  companion object {
    private const val serialVersionUID = 0L
  }

}