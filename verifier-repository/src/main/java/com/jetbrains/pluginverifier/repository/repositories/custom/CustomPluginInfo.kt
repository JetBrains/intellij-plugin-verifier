package com.jetbrains.pluginverifier.repository.repositories.custom

import com.jetbrains.pluginverifier.misc.safeEquals
import com.jetbrains.pluginverifier.misc.safeHashCode
import com.jetbrains.pluginverifier.repository.Browseable
import com.jetbrains.pluginverifier.repository.Downloadable
import com.jetbrains.pluginverifier.repository.PluginInfo
import java.net.URL
import java.util.*

/**
 * [PluginInfo] for a plugin from [CustomPluginRepository].
 */
class CustomPluginInfo(
    pluginId: String,
    pluginName: String,
    version: String,
    vendor: String,
    override val downloadUrl: URL,
    override val browserUrl: URL
) : Downloadable, Browseable, PluginInfo(
    pluginId,
    pluginName,
    version,
    null,
    null,
    vendor
) {

  override fun equals(other: Any?) = other is CustomPluginInfo
      && pluginId == other.pluginId
      && version == other.version
      && downloadUrl.safeEquals(other.downloadUrl)

  override fun hashCode() = Objects.hash(pluginId, version, downloadUrl.safeHashCode())
}