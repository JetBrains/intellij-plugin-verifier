package com.jetbrains.plugin.structure.teamcity.action.model

import com.jetbrains.plugin.structure.base.plugin.Plugin
import com.jetbrains.plugin.structure.base.plugin.PluginIcon
import com.jetbrains.plugin.structure.base.plugin.ThirdPartyDependency
import com.vdurmont.semver4j.Semver

data class TeamCityActionPlugin(
  override val pluginName: String,
  override val description: String,
  override var vendor: String? = null,
  override var vendorEmail: String? = null,
  override var vendorUrl: String? = null,
  override val icons: List<PluginIcon> = emptyList(),
  override val pluginId: String,
  override val pluginVersion: String,
  override val thirdPartyDependencies: List<ThirdPartyDependency> = emptyList(),
  override val url: String? = null,
  override val changeNotes: String? = null,

  val specVersion: Semver,
  val inputs: List<ActionInput> = emptyList(),
  val requirements: List<ActionRequirement> = emptyList(),
  val steps: List<ActionStep> = emptyList(),
) : Plugin