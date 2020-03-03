package com.jetbrains.plugin.structure.intellij.plugin

import kotlinx.serialization.Serializable

@Serializable
data class IdeTheme(val name: String, val dark: Boolean)