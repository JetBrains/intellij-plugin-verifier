package com.jetbrains.plugin.structure.youtrack

import com.jetbrains.plugin.structure.base.plugin.Plugin
import com.jetbrains.plugin.structure.base.plugin.PluginIcon
import com.jetbrains.plugin.structure.base.plugin.ThirdPartyDependency

data class YouTrackPlugin(
  override val pluginId: String? = null,
  override val pluginName: String? = null,
  override val pluginVersion: String? = null,
  override val description: String? = null,
  override val url: String? = null,
  override var vendor: String? = null,
  override var vendorUrl: String? = null,
  override var vendorEmail: String? = null,
  override val icons: List<PluginIcon> = emptyList(),
  override val changeNotes: String? = null,
  override val thirdPartyDependencies: List<ThirdPartyDependency> = emptyList(),
  val sinceVersion: String? = null,
  val untilVersion: String? = null,
) : Plugin