package com.jetbrains.plugin.structure.hub

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.jetbrains.plugin.structure.base.plugin.Plugin
import com.jetbrains.plugin.structure.base.plugin.PluginIcon

@JsonIgnoreProperties(ignoreUnknown = true)
data class HubPlugin(
    @JsonProperty("key")
    override val pluginId: String? = null,
    @JsonProperty("name")
    override val pluginName: String? = null,
    @JsonProperty("version")
    override val pluginVersion: String? = null,
    @JsonProperty("homeUrl")
    override val url: String = "",
    @JsonProperty("description")
    override val description: String? = null,

    @JsonProperty("author")
    val author: String = "",
    @JsonProperty("iconUrl")
    val iconUrl: String? = null,
    @JsonProperty("dependencies")
    val dependencies: Map<String, String>? = null,
    @JsonProperty("products")
    val products: Map<String, String>? = null

) : Plugin {
  var manifestContent: String = ""
  override val icons: List<PluginIcon> = emptyList()
  override var vendorUrl: String? = null
  override var vendor: String? = null
  override var vendorEmail: String? = null
  override val changeNotes: String? = null
}

data class VendorInfo(val vendor: String? = null, val vendorEmail: String = "", val vendorUrl: String = "")