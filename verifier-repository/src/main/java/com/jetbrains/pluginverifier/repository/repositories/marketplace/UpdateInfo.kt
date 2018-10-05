package com.jetbrains.pluginverifier.repository.repositories.marketplace

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.repository.Browseable
import com.jetbrains.pluginverifier.repository.Downloadable
import com.jetbrains.pluginverifier.repository.PluginInfo
import java.net.URL
import java.util.*

/**
 * Identifier of a plugin hosted on the public Plugin Repository [MarketplaceRepository].
 */
class UpdateInfo(
    pluginId: String,
    val pluginIntId: Int,
    pluginName: String,
    version: String,
    sinceBuild: IdeVersion?,
    untilBuild: IdeVersion?,
    vendor: String,
    override val downloadUrl: URL,
    val updateId: Int,
    override val browserUrl: URL,
    val tags: List<String>
) : Downloadable, Browseable, PluginInfo(
    pluginId,
    pluginName,
    version,
    sinceBuild,
    untilBuild,
    vendor
) {

  override val presentableName
    get() = "$pluginId:$version (#$updateId)"

  override fun equals(other: Any?) = other is UpdateInfo
      && pluginId == other.pluginId
      && version == other.version
      && updateId == other.updateId

  override fun hashCode() = Objects.hash(pluginId, version, updateId)

  companion object {
    private const val serialVersionUID = 0L
  }

}