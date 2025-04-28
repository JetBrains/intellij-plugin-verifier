/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin.dependencies

import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin

typealias PluginId = String

private const val UNKNOWN_DEPENDENCY_ID = "<unknown dependency>"

internal val IdePlugin.id: String
  get() {
    return this.pluginId ?: this.pluginName ?: UNKNOWN_DEPENDENCY_ID
  }

internal val Dependency.id: String
  get() {
    return when (this) {
      is Dependency.Module -> this.plugin.id
      is Dependency.Plugin -> this.plugin.id
      Dependency.None -> null
    } ?: UNKNOWN_DEPENDENCY_ID
  }