/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.teamcity

import com.jetbrains.plugin.structure.base.plugin.Plugin
import com.jetbrains.plugin.structure.base.plugin.PluginIcon
import com.jetbrains.plugin.structure.base.plugin.ThirdPartyDependency
import com.jetbrains.plugin.structure.teamcity.beans.TeamcityPluginBean

data class TeamcityPlugin(
  override val pluginId: String,
  override val pluginName: String,
  override val pluginVersion: String,
  override val url: String?,
  override val changeNotes: String?,
  override val description: String?,
  override val vendor: String?,
  override val vendorEmail: String?,
  override val vendorUrl: String?,
  override val thirdPartyDependencies: List<ThirdPartyDependency> = emptyList(),
  val sinceBuild: TeamcityVersion?,
  val untilBuild: TeamcityVersion?,
  val downloadUrl: String?,
  val useSeparateClassLoader: Boolean,
  val parameters: Map<String, String>?
) : Plugin {
  override val icons: List<PluginIcon> = emptyList()
}


fun TeamcityPluginBean.toPlugin() = TeamcityPlugin(
  pluginId = "teamcity_" + this.info?.name!!,
  pluginName = this.info?.displayName!!,
  pluginVersion = this.info?.version!!,
  url = null,
  changeNotes = null,
  description = this.info?.description,
  vendor = this.info?.vendor?.name,
  vendorEmail = this.info?.email,
  vendorUrl = this.info?.vendor?.url,
  sinceBuild = this.requirements?.minBuild?.toLong()?.let { TeamcityVersion(it) },
  untilBuild = this.requirements?.maxBuild?.toLong()?.let { TeamcityVersion(it) },
  downloadUrl = this.info?.downloadUrl,
  useSeparateClassLoader = this.deployment?.useSeparateClassLoader?.toBoolean() ?: false,
  parameters = this.parameters.associate { it.name!! to it.value!! }
)