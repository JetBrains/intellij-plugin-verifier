package com.jetbrains.pluginverifier.repository

import java.util.*

/**
 * Identifier of an abstract plugin, which may
 * represent either a [plugin from the Plugin Repository] [UpdateInfo],
 * or a [locally stored plugin] [com.jetbrains.pluginverifier.repository.local.LocalPluginInfo].
 */
open class PluginInfo(
    val pluginId: String,

    val version: String,

    val pluginRepository: PluginRepository
) {

  open val presentableName: String = "$pluginId $version"

  final override fun equals(other: Any?) = other is PluginInfo &&
      pluginId == other.pluginId && version == other.version && pluginRepository == other.pluginRepository

  final override fun hashCode() = Objects.hash(pluginId, version, pluginRepository)

  final override fun toString() = presentableName

}