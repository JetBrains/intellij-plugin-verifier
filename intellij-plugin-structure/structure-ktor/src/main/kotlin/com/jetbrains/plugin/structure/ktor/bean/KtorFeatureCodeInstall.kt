/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ktor.bean

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FeatureInstallReceipt(
        @SerialName(INSTALL_IMPORTS)
        val imports: List<String> = emptyList(),
        @SerialName(INSTALL_BLOCK)
        val installBlock: String? = null,
        @SerialName(INSTALL_TEMPLATES)
        val extraTemplates: List<CodeTemplate> = emptyList()
)

@Serializable
enum class Position {
    @SerialName(POSITION_INSIDE)
    INSIDE_APPLICATION_MODULE,

    @SerialName(POSITION_OUTSIDE)
    OUTSIDE_APPLICATION_MODULE,

    @SerialName(POSITION_FILE)
    SEPARATE_FILE,

    @SerialName(POSITION_TESTFUN)
    TEST_FUNCTION
}

@Serializable
data class CodeTemplate(
        @SerialName(TEMPLATE_POSITION)
        val position: Position? = null,
        @SerialName(TEMPLATE_TEXT)
        val text: String? = null
)