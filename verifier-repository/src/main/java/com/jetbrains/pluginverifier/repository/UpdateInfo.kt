package com.jetbrains.pluginverifier.repository

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import java.net.URL

/**
 * Aggregates properties of a plugin stored in the Plugin Repository.
 */
class UpdateInfo(
    pluginId: String,
    pluginName: String,
    version: String,
    pluginRepository: PluginRepository,
    sinceBuild: IdeVersion?,
    untilBuild: IdeVersion?,
    vendor: String,
    downloadUrl: URL,
    val updateId: Int,
    val browserURL: URL
) : PluginInfo(
    pluginId,
    pluginName,
    version,
    pluginRepository,
    sinceBuild,
    untilBuild,
    vendor,
    downloadUrl,
    null
) {

  override val presentableName: String = "$pluginId:$version (#$updateId)"

}