/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

data class ModuleV2Dependency(private val id: String) : PluginDependency {
  override fun getId() = id

  override fun isOptional() = true

  override fun isModule() = true

  override fun toString() = "$id (module, v2)"
}