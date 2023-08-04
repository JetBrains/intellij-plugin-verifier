/*
 * Copyright 2000-2021 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.fleet

import com.jetbrains.plugin.structure.base.plugin.Plugin
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
  val compatibleShipVersionRange: FleetShipVersionRange,
  val descriptorFileName: String,
  val files: List<PluginFile>
) : Plugin {
  override val changeNotes: String? = null
  override val vendorEmail: String? = null
  override val vendorUrl: String? = null
  override val url: String? = null
}

data class PluginFile(val name: String, val content: ByteArray) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as PluginFile

    if (name != other.name) return false
    if (!content.contentEquals(other.content)) return false

    return true
  }

  override fun hashCode(): Int {
    var result = name.hashCode()
    result = 31 * result + content.contentHashCode()
    return result
  }
}