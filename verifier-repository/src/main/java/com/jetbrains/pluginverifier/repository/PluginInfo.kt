package com.jetbrains.pluginverifier.repository

import java.util.*

/**
 * Identifier of an abstract plugin, which may
 * represent either a plugin in the Plugin Repository ([UpdateInfo]),
 * or a locally stored file ([LocalPluginInfo]).
 */
open class PluginInfo(
    val pluginId: String,

    val version: String
) {

  override fun equals(other: Any?) =
      other is PluginInfo && pluginId == other.pluginId && version == other.version

  override fun hashCode() = Objects.hash(pluginId, version)
}