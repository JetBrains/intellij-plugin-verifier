/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ktor

import com.jetbrains.plugin.structure.base.plugin.Plugin
import com.jetbrains.plugin.structure.base.plugin.PluginIcon

data class KtorFeature(
  override val pluginName: String? = null,
  override val description: String? = null,
  override var vendor: String? = null,
  override var vendorEmail: String? = null,
  override var vendorUrl: String? = null,
  override val icons: List<PluginIcon> = emptyList(),
  override val pluginVersion: String? = null,
  override val pluginId: String? = null,
  val requiredFeatures: List<String> = emptyList(), // Feature IDs.
  val documentation: KtorFeatureDocumentation? = null,
  val fullDescriptorJson: String = ""
) : Plugin {
  override val url: String = ""
  override val changeNotes: String? = null
}

data class KtorFeatureDocumentation(
  val description: String,
  val usage: String,
  val options: String
)