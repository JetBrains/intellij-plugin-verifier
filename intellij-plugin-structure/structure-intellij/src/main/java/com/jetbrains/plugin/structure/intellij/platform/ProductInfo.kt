package com.jetbrains.plugin.structure.intellij.platform

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.nio.file.Path

@JsonIgnoreProperties(ignoreUnknown = true)
data class ProductInfo(
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
  @JsonProperty("layout") val layout: List<LayoutComponent>
)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "kind")
@JsonSubTypes(
  JsonSubTypes.Type(value = LayoutComponent.Plugin::class, name = "plugin"),
  JsonSubTypes.Type(value = LayoutComponent.PluginAlias::class, name = "pluginAlias"),
  JsonSubTypes.Type(value = LayoutComponent.ModuleV2::class, name = "moduleV2"),
  JsonSubTypes.Type(value = LayoutComponent.ProductModuleV2::class, name = "productModuleV2")
)

sealed class LayoutComponent(val kind: String) {
  abstract val name: String

  interface Classpathable {
    @JsonIgnore
    fun getClasspath(): List<Path>
  }

  data class Plugin(
    @JsonProperty("name") override val name: String,
    @JsonProperty("classPath")
    val classPaths: List<String>,
  ) : LayoutComponent("plugin"), Classpathable {
    override fun getClasspath() = classPaths.paths
  }

  data class PluginAlias(
    @JsonProperty("name") override val name: String,
  ) : LayoutComponent("pluginAlias")

  data class ModuleV2(
    override val name: String,
    val classPaths: List<String>,
  ) : LayoutComponent("moduleV2"), Classpathable {

    companion object {
      @JvmStatic
      @JsonCreator
      fun create(@JsonProperty("name") name: String, @JsonProperty("classPath") classPaths: List<String>?): ModuleV2 {
        return ModuleV2(name, classPaths ?: emptyList())
      }
    }

    override fun getClasspath() = classPaths.paths
  }

  data class ProductModuleV2(
    @JsonProperty("name") override val name: String,
    @JsonProperty("classPath")
    val classPaths: List<String>,
  ) : LayoutComponent("productModuleV2"), Classpathable {
    override fun getClasspath() = classPaths.paths
  }

  protected val List<String>.paths
    get() = map { Path.of(it) }
}



