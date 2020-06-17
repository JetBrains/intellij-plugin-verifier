/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.edu.bean

import com.jetbrains.plugin.structure.edu.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EduPluginDescriptor(
    @SerialName(TITLE)
    val title: String? = null,
    @SerialName(SUMMARY)
    val summary: String? = null,
    @SerialName(LANGUAGE)
    val language: String? = null,
    @SerialName(PROGRAMMING_LANGUAGE)
    val programmingLanguage: String? = null,
    @SerialName(ITEMS)
    val items: List<String>? = null,
    @SerialName(VERSION)
    val courseVersion: String? = null,

    // format example: 3.7-2019.3-5266  -- plugin version-ide version-build number
    @SerialName(EDU_PLUGIN_VERSION)
    val eduPluginVersion: String? = null
)