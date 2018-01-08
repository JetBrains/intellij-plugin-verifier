package com.jetbrains.pluginverifier.repository

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.misc.safeEquals
import com.jetbrains.pluginverifier.misc.safeHashCode
import java.util.*

/**
 * Identifier of an abstract plugin, which may
 * represent either a [plugin from the Plugin Repository] [UpdateInfo],
 * or a [locally stored plugin] [com.jetbrains.pluginverifier.repository.local.LocalPluginInfo].
 */
open class PluginInfo(
    val pluginId: String,

    val pluginName: String,

    val version: String,

    val pluginRepository: PluginRepository,

    val sinceBuild: IdeVersion?,

    val untilBuild: IdeVersion?,

    val vendor: String?
) {

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