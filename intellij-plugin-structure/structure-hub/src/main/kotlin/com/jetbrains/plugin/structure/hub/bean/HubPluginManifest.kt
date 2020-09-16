/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.hub.bean

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class HubPluginManifest(
    @JsonProperty("key")
    val pluginId: String? = null,
    @JsonProperty("name")
    val pluginName: String? = null,
    @JsonProperty("version")
    val pluginVersion: String? = null,
    @JsonProperty("homeUrl")
    val url: String = "",
    @JsonProperty("description")
    val description: String? = null,
    @JsonProperty("author")
    val author: String? = null,
    @JsonProperty("iconUrl")
    val iconUrl: String? = null,
    @JsonProperty("dependencies")
    val dependencies: Map<String, String>? = null,
    @JsonProperty("products")
    val products: Map<String, String>? = null
)