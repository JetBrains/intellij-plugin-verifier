package com.jetbrains.plugin.structure.teamcity

import com.jetbrains.plugin.structure.base.plugin.Plugin
import com.jetbrains.plugin.structure.base.plugin.PluginIcon
import com.jetbrains.plugin.structure.teamcity.beans.TeamcityPluginBean

data class TeamcityPlugin(
    override val pluginId: String,
    override val pluginName: String,
    override val pluginVersion: String,
    override val url: String?,
    override val changeNotes: String?,
    override val description: String?,
    override val vendor: String?,
    override val vendorEmail: String?,
    override val vendorUrl: String?,
    val sinceBuild: TeamcityVersion?,
    val untilBuild: TeamcityVersion?,
    val downloadUrl: String?,
    val useSeparateClassLoader: Boolean,
    val parameters: Map<String, String>?
) : Plugin {
  override val icons: List<PluginIcon> = emptyList()
}


fun TeamcityPluginBean.toPlugin() = TeamcityPlugin(
    pluginId = "teamcity_" + this.name!!,
    pluginName = this.displayName!!,
    pluginVersion = this.version!!,
    url = null,
    changeNotes = null,
    description = this.description,
    vendor = this.vendor?.name,
    vendorEmail = this.email,
    vendorUrl = this.vendor?.url,
    sinceBuild = this.minBuild?.toLong()?.let { TeamcityVersion(it) },
    untilBuild = this.maxBuild?.toLong()?.let { TeamcityVersion(it) },
    downloadUrl = this.downloadUrl,
    useSeparateClassLoader = this.useSeparateClassLoader?.toBoolean() ?: false,
    parameters = this.parameters?.associate { it.name!! to it.value!! }
)