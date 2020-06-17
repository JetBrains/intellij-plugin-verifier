/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.edu.bean

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EduPluginDescriptor(
    @SerialName("title")
    val title: String? = null,
    @SerialName("summary")
    val summary: String? = null,
    @SerialName("version")
    val courseVersion: String? = null,
    @SerialName("language")
    val language: String? = null,
    @SerialName("programming_language")
    val programmingLanguage: String? = null
)