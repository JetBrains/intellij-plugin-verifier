/*
 * Copyright 2000-2021 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.fleet

import com.jetbrains.plugin.structure.base.plugin.Plugin
import com.jetbrains.plugin.structure.base.plugin.PluginIcon
import com.jetbrains.plugin.structure.fleet.bean.FleetDependency

data class FleetPlugin(
  override val pluginId: String? = null,
  override val pluginName: String? = null,
  override val pluginVersion: String? = null,
  override val icons: List<PluginIcon> = emptyList(),
  override val description: String? = null,
  override val vendor: String? = null,
  val entryPoint: String? = null,
  val requires: List<FleetDependency>? = null
) : Plugin {
  override val changeNotes: String? = null
  override val vendorEmail: String? = null
  override val vendorUrl: String? = null
  override val url: String? = null
}