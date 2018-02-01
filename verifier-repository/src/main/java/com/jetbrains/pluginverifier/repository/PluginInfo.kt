package com.jetbrains.pluginverifier.repository

import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.misc.safeEquals
import com.jetbrains.pluginverifier.misc.safeHashCode
import java.net.URL
import java.util.*

/**
 * Identifier of a IDE plugin, which may
 * represent either a [plugin from the Plugin Repository] [UpdateInfo],
 * a [locally stored plugin] [com.jetbrains.pluginverifier.repository.local.LocalPluginInfo],
 * or a [bundled] [com.jetbrains.pluginverifier.repository.bundled.BundledPluginInfo] IDE plugin.
 */
open class PluginInfo(
    val pluginId: String,

    val pluginName: String,

    val version: String,

    val pluginRepository: PluginRepository,

    val sinceBuild: IdeVersion?,

    val untilBuild: IdeVersion?,

    val vendor: String?,

    /**
     * URL that can be used to download the plugin's file.
     * It can be `null` only if _this_ is an in-memory plugin
     * without a backed file. In this case the [idePlugin] is not null.
     */
    val downloadUrl: URL?,

    /**
     * Descriptor of this [IdePlugin] if it is already opened,
     * like in case of a [bundled] [BundledPluginInfo] or
     * a [local] [LocalPluginInfo] plugin.
     */
    val idePlugin: IdePlugin?
) {

  init {
    require(downloadUrl != null || idePlugin != null)
  }

  fun isCompatibleWith(ideVersion: IdeVersion) =
      (sinceBuild == null || sinceBuild <= ideVersion) && (untilBuild == null || ideVersion <= untilBuild)

  open val presentableName: String = "$pluginId $version"

  final override fun equals(other: Any?) = other is PluginInfo
      && pluginId == other.pluginId
      && version == other.version
      && pluginRepository.repositoryURL.safeEquals(other.pluginRepository.repositoryURL)

  final override fun hashCode() = Objects.hash(pluginId, version, pluginRepository.repositoryURL.safeHashCode())

  final override fun toString() = presentableName

}