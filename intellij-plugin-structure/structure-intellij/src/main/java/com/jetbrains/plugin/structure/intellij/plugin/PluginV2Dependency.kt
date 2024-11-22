/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

data class PluginV2Dependency(private val id: String) : PluginDependency {
  override fun getId() = id

  override fun isOptional() = false

  override fun isModule() = id.startsWith(INTELLIJ_MODULE_PREFIX)

  override fun toString(): String {
    val moduleFlag = if (isModule) "+module" else ""
    return "$id (plugin$moduleFlag, v2)"
  }
}