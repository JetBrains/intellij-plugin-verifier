/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.edu

import com.jetbrains.plugin.structure.base.plugin.Plugin
import com.jetbrains.plugin.structure.base.plugin.PluginIcon

data class EduPlugin(
    override val pluginId: String? = null,
    override val pluginName: String? = null,
    override val pluginVersion: String? = null,
    override val url: String = "",
    override val description: String? = null,
    override var vendor: String? = null,
    override var vendorUrl: String? = null,
    override var vendorEmail: String? = null,
    override val icons: List<PluginIcon> = emptyList(),
    val language: String? = null
) : Plugin {
  override val changeNotes: String? = null
}