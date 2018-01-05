package com.jetbrains.pluginverifier.repository

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import java.net.URL
import java.util.*

/**
 * Aggregates properties of a plugin stored in the Plugin Repository.
 */
class UpdateInfo(pluginId: String,
                 version: String,
                 val pluginName: String,
                 val updateId: Int,
                 val vendor: String,
                 val sinceString: String,
                 val untilString: String,
                 val downloadUrl: URL,
                 val browserURL: URL,
                 val repositoryURL: URL) : PluginInfo(pluginId, version) {

  override fun toString(): String = "$pluginId:$version (#$updateId)"

  override fun equals(other: Any?) = other is UpdateInfo && repositoryURL == other.repositoryURL && updateId == other.updateId

  override fun hashCode() = Objects.hash(repositoryURL, updateId)

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