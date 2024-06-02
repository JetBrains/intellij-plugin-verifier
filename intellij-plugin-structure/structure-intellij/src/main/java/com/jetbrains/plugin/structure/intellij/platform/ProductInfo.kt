package com.jetbrains.plugin.structure.intellij.platform

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class ProductInfo(
  @JsonProperty("name") val name: String,
  @JsonProperty("version") val version: String,
  @JsonProperty("versionSuffix") val versionSuffix: String,
  @JsonProperty("buildNumber") val buildNumber: String,
  @JsonProperty("productCode") val productCode: String,
  @JsonProperty("dataDirectoryName") val dataDirectoryName: String,
  @JsonProperty("svgIconPath") val svgIconPath: String,
  @JsonProperty("productVendor") val productVendor: String,
  @JsonProperty("bundledPlugins") val bundledPlugins: List<String>,
  @JsonProperty("modules") val modules: List<String>,
  @JsonProperty("layout") val layout: List<Layout>
)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "kind")
@JsonSubTypes(
  JsonSubTypes.Type(value = Layout.Plugin::class, name = "plugin"),
  JsonSubTypes.Type(value = Layout.PluginAlias::class, name = "pluginAlias"),
  JsonSubTypes.Type(value = Layout.ModuleV2::class, name = "moduleV2"),
  JsonSubTypes.Type(value = Layout.ProductModuleV2::class, name = "productModuleV2")
)

internal sealed class Layout(val kind: String) {
  abstract val name: String

  internal data class Plugin(
    @JsonProperty("name") override val name: String,
    @JsonProperty("classPath")
    val classPaths: List<String>,
  ) : Layout("plugin")

  internal data class PluginAlias(
    @JsonProperty("name") override val name: String,
  ) : Layout("pluginAlias")

  internal data class ModuleV2(
    override val name: String,
    val classPaths: List<String>,
  ) : Layout("moduleV2") {

    companion object {
      @JvmStatic
      @JsonCreator
      fun create(@JsonProperty("name") name: String, @JsonProperty("classPath") classPaths: List<String>?): ModuleV2 {
        return ModuleV2(name, classPaths ?: emptyList())
      }
    }
  }

  internal data class ProductModuleV2(
    @JsonProperty("name") override val name: String,
    @JsonProperty("classPath")
    val classPaths: List<String>,
  ) : Layout("productModuleV2")
}



