package com.jetbrains.plugin.structure.teamcity.recipe

import com.jetbrains.plugin.structure.base.plugin.Plugin
import com.jetbrains.plugin.structure.base.plugin.PluginFile
import com.jetbrains.plugin.structure.base.plugin.PluginIcon
import com.jetbrains.plugin.structure.base.plugin.ThirdPartyDependency

data class TeamCityRecipePlugin(
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
  val namespace: String,
  val specVersion: String,
  val dependencies: List<TeamCityRecipeDependency>,
  val yamlFile: PluginFile,
) : Plugin