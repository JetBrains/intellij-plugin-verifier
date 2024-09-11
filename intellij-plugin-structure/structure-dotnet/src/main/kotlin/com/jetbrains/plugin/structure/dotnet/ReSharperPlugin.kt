/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.dotnet

import com.jetbrains.plugin.structure.base.plugin.Plugin
import com.jetbrains.plugin.structure.base.plugin.PluginFile
import com.jetbrains.plugin.structure.base.plugin.PluginIcon
import com.jetbrains.plugin.structure.base.plugin.ThirdPartyDependency

class ReSharperPlugin(
  override val pluginId: String,
  override val pluginName: String,
  override val url: String?,
  override val changeNotes: String?,
  override val description: String?,
  override val vendor: String?,
  override val vendorEmail: String?,
  override val vendorUrl: String?,
  override val thirdPartyDependencies: List<ThirdPartyDependency> = emptyList(),
  val nonNormalizedVersion: String,
  val summary: String?,
  val authors: List<String>,
  val licenseUrl: String?,
  val copyright: String?,
  val dependencies: List<DotNetDependency>,
  val nuspecFile: PluginFile
) : Plugin {
  override val icons: List<PluginIcon> = emptyList()
  val parsedVersion = NugetSemanticVersion.parse(nonNormalizedVersion)
  override val pluginVersion = parsedVersion.normalizedVersionString
}

data class DotNetDependency(val id: String, val versionRange: String?)