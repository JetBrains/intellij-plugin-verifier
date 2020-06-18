/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.edu

import com.jetbrains.plugin.structure.base.plugin.Plugin
import com.jetbrains.plugin.structure.base.plugin.PluginIcon

data class EduPlugin(
  override val pluginName: String? = null,
  override val description: String? = null,
  override var vendor: String? = null,

  val language: String? = null,
  val programmingLanguage: String? = null,
  val eduPluginVersion: String?,
  val items: List<String> = emptyList()

) : Plugin {
  override val pluginId: String? = null
  override val pluginVersion: String? = null
  override val url: String = ""
  override var vendorUrl: String? = null
  override var vendorEmail: String? = null
  override val icons: List<PluginIcon> = emptyList()
  override val changeNotes: String? = null

  val parsedEduVersion = EduPluginVersion.parse(eduPluginVersion)
}