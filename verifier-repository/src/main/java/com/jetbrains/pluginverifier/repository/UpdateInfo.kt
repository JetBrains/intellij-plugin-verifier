package com.jetbrains.pluginverifier.repository

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import java.net.URL

/**
 * Aggregates properties of a plugin stored in the Plugin Repository.
 */
data class UpdateInfo(override val pluginId: String,
                      override val version: String,
                      val pluginName: String,
                      val updateId: Int,
                      val vendor: String,
                      val sinceString: String,
                      val untilString: String,
                      val downloadUrl: URL,
                      val browserURL: URL,
                      val repositoryURL: URL) : PluginInfo {

  override fun toString(): String = "$pluginId:$version (#$updateId)"

  override fun equals(other: Any?) = other is UpdateInfo && updateId == other.updateId

  override fun hashCode() = updateId

  val sinceBuild: IdeVersion?
    get() = sinceString.prepareIdeVersion()

  val untilBuild: IdeVersion?
    get() = untilString.prepareIdeVersion()

  fun isCompatibleWith(ideVersion: IdeVersion): Boolean {
    val since = sinceBuild
    val until = untilBuild
    return (since == null || since <= ideVersion) && (until == null || ideVersion <= until)
  }

  private fun String.prepareIdeVersion(): IdeVersion? {
    if (this == "" || this == "0.0") {
      return null
    }
    return IdeVersion.createIdeVersionIfValid(this)
  }
}