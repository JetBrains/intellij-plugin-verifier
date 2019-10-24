package com.jetbrains.pluginverifier.repository

import com.jetbrains.plugin.structure.intellij.version.IdeVersion

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
) {

  /**
   * Checks whether this plugin is compatible with [ideVersion].
   */
  fun isCompatibleWith(ideVersion: IdeVersion) =
    (sinceBuild == null || sinceBuild <= ideVersion) && (untilBuild == null || ideVersion <= untilBuild)

  val presentableSinceUntilRange: String
    get() {
      val sinceCode = sinceBuild?.asStringWithoutProductCode()
      val untilCode = untilBuild?.asStringWithoutProductCode()
      if (sinceCode != null) {
        if (untilCode != null) {
          return "$sinceCode — $untilCode"
        }
        return "$sinceCode+"
      }
      if (untilCode != null) {
        return "1.0 — $untilCode"
      }
      return "all"
    }

  open val presentableName
    get() = "$pluginId $version"

  final override fun toString() = presentableName
}