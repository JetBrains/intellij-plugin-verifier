package com.jetbrains.plugin.structure.dotnet

import com.jetbrains.plugin.structure.base.plugin.Plugin
import com.jetbrains.plugin.structure.base.plugin.PluginIcon

data class ReSharperPlugin(
    override val pluginId: String,
    override val pluginName: String,
    override val pluginVersion: String,
    override val url: String?,
    override val changeNotes: String?,
    override val description: String?,
    override val vendor: String?,
    override val vendorEmail: String?,
    override val vendorUrl: String?,

    val summary: String?,
    val authors: List<String>,
    val licenseUrl: String?,
    val copyright: String?,
    val dependencies: List<DotNetDependency>
) : Plugin {
  override val icons: List<PluginIcon> = emptyList()
}

data class DotNetDependency(val id: String, val versionRange: String)