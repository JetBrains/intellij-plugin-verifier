package com.jetbrains.plugin.structure.youtrack.bean

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class YouTrackAppManifest(

  @JsonProperty(YouTrackAppFields.Manifest.NAME)
  val name: String? = null,

  @JsonProperty(YouTrackAppFields.Manifest.TITLE)
  val title: String? = null,

  @JsonProperty(YouTrackAppFields.Manifest.VERSION)
  val version: String? = null,

  @JsonProperty(YouTrackAppFields.Manifest.DESCRIPTION)
  val description: String? = null,

  @JsonProperty(YouTrackAppFields.Manifest.URL)
  val url: String? = null,

  @JsonProperty(YouTrackAppFields.Manifest.ICON)
  val icon: String? = null,

  @JsonProperty(YouTrackAppFields.Manifest.ICON_DARK)
  val iconDark: String? = null,

  @JsonProperty(YouTrackAppFields.Manifest.SINCE)
  val minYouTrackVersion: String? = null,

  @JsonProperty(YouTrackAppFields.Manifest.UNTIL)
  val maxYouTrackVersion: String? = null,

  @JsonProperty(YouTrackAppFields.Manifest.NOTES)
  val changeNotes: String? = null,

  @JsonProperty(YouTrackAppFields.Manifest.VENDOR)
  val vendor: YouTrackAppVendor? = null,

  @JsonProperty(YouTrackAppFields.Manifest.WIDGETS)
  val widgets: List<YouTrackAppWidget>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class YouTrackAppWidget(

  @JsonProperty(YouTrackAppFields.Widget.KEY)
  val key: String? = null,

  @JsonProperty(YouTrackAppFields.Widget.EXTENSION_POINT)
  val extensionPoint: String? = null,

  @JsonProperty(YouTrackAppFields.Widget.INDEX_PATH)
  val indexPath: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class YouTrackAppVendor(

  @JsonProperty(YouTrackAppFields.Vendor.NAME)
  val name: String? = null,

  @JsonProperty(YouTrackAppFields.Vendor.URL)
  val url: String? = null,

  @JsonProperty(YouTrackAppFields.Vendor.EMAIL)
  val email: String? = null
)