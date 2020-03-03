package com.jetbrains.plugin.structure.hub

import com.jetbrains.plugin.structure.base.plugin.Plugin
import com.jetbrains.plugin.structure.base.plugin.PluginIcon
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class HubPlugin(
    @SerialName("key")
  override val pluginId: String? = null,
    @SerialName("name")
  override val pluginName: String? = null,
    @SerialName("version")
  override val pluginVersion: String? = null,
    @SerialName("homeUrl")
  override val url: String = "",
    @SerialName("description")
  override val description: String? = null,

    @SerialName("author")
  val author: String = "",
    @SerialName("iconUrl")
  val iconUrl: String? = null,
    @SerialName("dependencies")
  val dependencies: Map<String, String>? = null,
    @SerialName("products")
  val products: Map<String, String>? = null

) : Plugin {
  var manifestContent: String = ""
  @Transient
  override val icons: List<PluginIcon> = emptyList()
  override var vendorUrl: String? = null
  override var vendor: String? = null
  override var vendorEmail: String? = null
  override val changeNotes: String? = null
}

data class VendorInfo(val vendor: String? = null, val vendorEmail: String = "", val vendorUrl: String = "")