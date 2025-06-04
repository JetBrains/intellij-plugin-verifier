/*
 * Copyright 2000-2021 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.fleet

import com.jetbrains.plugin.structure.base.plugin.Plugin
import com.jetbrains.plugin.structure.base.plugin.PluginFile
import com.jetbrains.plugin.structure.base.plugin.PluginIcon
import com.jetbrains.plugin.structure.base.plugin.ThirdPartyDependency

data class FleetPlugin(
  override val pluginId: String,
  override val pluginVersion: String,
  override val pluginName: String?,
  override val icons: List<PluginIcon>,
  override val description: String?,
  override val vendor: String?,
  override val thirdPartyDependencies: List<ThirdPartyDependency>,
  val frontendOnly: Boolean? = null,
  val humanVisible: Boolean,
  val supportedProducts: Set<String>,
  val compatibleShipVersionRange: FleetShipVersionRange,
  val descriptorFileName: String,
  val files: List<PluginFile>
) : Plugin {
  override val changeNotes: String? = null
  override val vendorEmail: String? = null
  override val vendorUrl: String? = null
  override val url: String? = null
}