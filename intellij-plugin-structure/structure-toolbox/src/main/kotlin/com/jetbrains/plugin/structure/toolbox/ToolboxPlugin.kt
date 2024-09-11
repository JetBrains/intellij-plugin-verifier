/*
 * Copyright 2000-2023 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.toolbox

import com.jetbrains.plugin.structure.base.plugin.Plugin
import com.jetbrains.plugin.structure.base.plugin.PluginFile
import com.jetbrains.plugin.structure.base.plugin.PluginIcon
import com.jetbrains.plugin.structure.base.plugin.ThirdPartyDependency

data class ToolboxPlugin(
  override val pluginId: String,
  override val pluginVersion: String,
  override val pluginName: String? = null,
  override val icons: List<PluginIcon> = emptyList(),
  override val description: String? = null,
  override val vendor: String? = null,
  override val thirdPartyDependencies: List<ThirdPartyDependency> = emptyList(),
  val compatibleVersionRange: ToolboxVersionRange,
  val descriptorFileName: String,
  val files: List<PluginFile>
) : Plugin {
  override val changeNotes: String? = null
  override val vendorEmail: String? = null
  override val vendorUrl: String? = null
  override val url: String? = null
}