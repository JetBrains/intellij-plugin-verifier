package com.jetbrains.plugin.structure.youtrack.bean

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class YouTrackAppManifest(

  @JsonProperty("name")
  val name: String? = null,

  @JsonProperty("title")
  val title: String? = null,

  @JsonProperty("version")
  val version: String? = null,

  @JsonProperty("description")
  val description: String? = null,

  @JsonProperty("url")
  val url: String? = null,

  @JsonProperty("icon")
  val icon: String? = null,

  @JsonProperty("iconDark")
  val iconDark: String? = null,

  @JsonProperty("minYouTrackVersion")
  val minYouTrackVersion: String? = null,

  @JsonProperty("maxYouTrackVersion")
  val maxYouTrackVersion: String? = null,

  @JsonProperty("changeNotes")
  val changeNotes: String? = null,

  @JsonProperty("vendor")
  val vendor: YouTrackAppVendor? = null,

  @JsonProperty("widgets")
  val widgets: List<YouTrackAppWidget>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class YouTrackAppWidget(

  @JsonProperty("key")
  val key: String? = null,

  @JsonProperty("extensionPoint")
  val extensionPoint: String? = null,

  @JsonProperty("indexPath")
  val indexPath: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class YouTrackAppVendor(

  @JsonProperty("name")
  val name: String? = null,

  @JsonProperty("url")
  val url: String? = null,

  @JsonProperty("email")
  val email: String? = null
)