package com.jetbrains.pluginverifier.repository

import java.net.URL

/**
 * Aggregates properties of a plugin stored in the Plugin Repository.
 */
data class UpdateInfo(override val pluginId: String,
                      override val version: String,
                      val pluginName: String,
                      val updateId: Int,
                      val vendor: String?,
                      val sinceBuild: String?,
                      val untilBuild: String?,
                      val downloadUrl: URL,
                      val browserURL: URL,
                      val repositoryURL: URL) : PluginInfo {

  override fun toString(): String = "$pluginId:$version (#$updateId)"

  override fun equals(other: Any?) = other is UpdateInfo && updateId == other.updateId

  override fun hashCode() = updateId
}