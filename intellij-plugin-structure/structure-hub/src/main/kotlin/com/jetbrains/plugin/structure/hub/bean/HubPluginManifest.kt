/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.hub.bean

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HubPluginManifest(
    @SerialName("key")
    val pluginId: String? = null,
    @SerialName("name")
    val pluginName: String? = null,
    @SerialName("version")
    val pluginVersion: String? = null,
    @SerialName("homeUrl")
    val url: String = "",
    @SerialName("description")
    val description: String? = null,
    @SerialName("author")
    val author: String? = null,
    @SerialName("iconUrl")
    val iconUrl: String? = null,
    @SerialName("dependencies")
    val dependencies: Map<String, String>? = null,
    @SerialName("products")
    val products: Map<String, String>? = null
)