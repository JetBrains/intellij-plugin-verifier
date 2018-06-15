package com.jetbrains.pluginverifier.repository

import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import java.io.InvalidObjectException
import java.io.ObjectInputStream
import java.io.Serializable
import java.net.URL
import java.util.*

/**
 * Identifier of an IDE plugin.
 */
open class PluginInfo(
    val pluginId: String,

    val version: String,

    val sinceBuild: IdeVersion?,

    val untilBuild: IdeVersion?,

    val vendor: String?
) : Serializable {

  fun isCompatibleWith(ideVersion: IdeVersion) =
      (sinceBuild == null || sinceBuild <= ideVersion) && (untilBuild == null || ideVersion <= untilBuild)

  open val presentableName
    get() = "$pluginId $version"

  override fun equals(other: Any?) = other is PluginInfo
      && pluginId == other.pluginId
      && version == other.version

  override fun hashCode() = Objects.hash(pluginId, version)

  final override fun toString() = presentableName

  companion object {
    private const val serialVersionUID = 0L
  }

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

  // writeReplace method for serialization
  private fun writeReplace(): Any = PluginInfo(pluginId, version, sinceBuild, untilBuild, vendor)

  // readObject method for serialization
  @Suppress("UNUSED_PARAMETER")
  private fun readObject(stream: ObjectInputStream): Unit = throw InvalidObjectException("Must have been serialized to PluginInfo")

  override fun equals(other: Any?) = other is LocalPluginInfo && idePlugin == other.idePlugin

  override fun hashCode() = idePlugin.hashCode()

  companion object {
    private const val serialVersionUID = 0L
  }

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
) {

  // writeReplace method for serialization
  private fun writeReplace(): Any = PluginInfo(pluginId, version, sinceBuild, untilBuild, vendor)

  // readObject method for serialization
  @Suppress("UNUSED_PARAMETER")
  private fun readObject(stream: ObjectInputStream): Unit = throw InvalidObjectException("Must have been serialized to PluginInfo")

  override fun equals(other: Any?) = other is BundledPluginInfo
      && ideVersion == other.ideVersion
      && idePlugin == other.idePlugin

  override fun hashCode() = Objects.hash(ideVersion, idePlugin)

  companion object {
    private const val serialVersionUID = 0L
  }

}

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