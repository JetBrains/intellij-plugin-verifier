package com.jetbrains.plugin.structure.teamcity

import com.jetbrains.plugin.structure.base.plugin.Plugin
import com.jetbrains.plugin.structure.teamcity.beans.TeamcityPluginBean

data class TeamcityPlugin(
    override val pluginId: String,
    override val pluginName: String,
    override val pluginVersion: String,
    override val sinceBuild: TeamcityVersion?,
    override val untilBuild: TeamcityVersion?,
    override val url: String?,
    override val changeNotes: String?,
    override val description: String?,
    override val vendor: String?,
    override val vendorEmail: String?,
    override val vendorUrl: String?,
    val downloadUrl: String?,
    val useSeparateClassLoader: Boolean,
    val parameters: Map<String, String>?
) : Plugin


fun TeamcityPluginBean.toPlugin() = TeamcityPlugin(
    pluginId = this.name!!,
    pluginName = this.displayName!!,
    pluginVersion = this.version!!,
    vendor = this.vendor?.name,
    vendorEmail = this.email,
    vendorUrl = this.vendor?.url,
    sinceBuild = this.minBuild?.toLong()?.let { TeamcityVersion(it) },
    untilBuild = this.maxBuild?.toLong()?.let { TeamcityVersion(it) },
    description = this.description,
    downloadUrl = this.downloadUrl,
    parameters = this.parameters?.associate { it.name!! to it.value!! },
    useSeparateClassLoader = this.useSeparateClassLoader?.toBoolean() ?: false,
    url = null,
    changeNotes = null
)