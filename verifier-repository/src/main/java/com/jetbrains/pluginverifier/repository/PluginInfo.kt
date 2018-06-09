package com.jetbrains.pluginverifier.repository

import java.net.URL
import java.util.*

/**
 * Identifier of an IDE plugin.
 */
sealed class PluginInfo(
    val pluginId: String,

    val version: String,

    val sinceBuild: IdeVersion?,

    val untilBuild: IdeVersion?,

    val vendor: String?
) {

  fun isCompatibleWith(ideVersion: IdeVersion) =
      (sinceBuild == null || sinceBuild <= ideVersion) && (untilBuild == null || ideVersion <= untilBuild)

  open val presentableName
    get() = "$pluginId $version"

  final override fun equals(other: Any?) = other is PluginInfo
      && pluginId == other.pluginId
      && version == other.version

  final override fun hashCode() = Objects.hash(pluginId, version)

  final override fun toString() = presentableName

}

/**
 * Identifier of a local plugin.
 */
class LocalPluginInfo(
    val idePlugin: IdePlugin
) : PluginInfo(
    idePlugin.pluginId!!,
    idePlugin.pluginVersion!!,
    idePlugin.sinceBuild,
    idePlugin.untilBuild,
    idePlugin.vendor
) {

  val definedModules: Set<String>
    get() = idePlugin.definedModules

  override val presentableName
    get() = idePlugin.toString()
}

/**
 * Identifier of a plugin bundled to IDE.
 */
class BundledPluginInfo(
    val ideVersion: IdeVersion,
    val idePlugin: IdePlugin
) : PluginInfo(
    idePlugin.pluginId!!,
    idePlugin.pluginVersion ?: ideVersion.asString(),
    idePlugin.sinceBuild,
    idePlugin.untilBuild,
    idePlugin.vendor
)

/**
 * Identifier of a plugin hosted in the Plugin Repository.
 */
class UpdateInfo(
    pluginId: String,
    version: String,
    sinceBuild: IdeVersion?,
    untilBuild: IdeVersion?,
    vendor: String,
    val downloadUrl: URL,
    val updateId: Int,
    val browserURL: URL,
    val tags: List<String>
) : PluginInfo(
    pluginId,
    version,
    sinceBuild,
    untilBuild,
    vendor
) {

  override val presentableName
    get() = "$pluginId:$version (#$updateId)"

}

/**
 * Only plugin ID and version.
 */
class PluginIdAndVersion(
    pluginId: String,
    version: String
) : PluginInfo(pluginId, version, null, null, null) {

  override val presentableName
    get() = "$pluginId $version"
}