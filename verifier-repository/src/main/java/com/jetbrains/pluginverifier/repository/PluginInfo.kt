package com.jetbrains.pluginverifier.repository

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import java.io.Serializable

/**
 * Identifier of a plugin.
 */
abstract class PluginInfo(
    /**
     * Unique plugin ID, which may be equal
     * to name if ID is not specified.
     */
    val pluginId: String,

    /**
     * Plugin name.
     */
    val pluginName: String,

    /**
     * Plugin version.
     */
    val version: String,

    /**
     * "since" compatibility range.
     */
    val sinceBuild: IdeVersion?,

    /**
     * "until" compatibility range.
     */
    val untilBuild: IdeVersion?,

    /**
     * Vendor of the plugin.
     */
    val vendor: String?
) : Serializable {

  /**
   * Checks whether this plugin is compatible with [ideVersion].
   */
  fun isCompatibleWith(ideVersion: IdeVersion) =
      (sinceBuild == null || sinceBuild <= ideVersion) && (untilBuild == null || ideVersion <= untilBuild)

  open val presentableName
    get() = "$pluginId $version"

  final override fun toString() = presentableName

  companion object {
    private const val serialVersionUID = 0L
  }

}