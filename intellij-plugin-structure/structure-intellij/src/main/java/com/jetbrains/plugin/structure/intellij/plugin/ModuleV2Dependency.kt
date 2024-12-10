/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

data class ModuleV2Dependency(override val id: String, override var isOptional: Boolean = false) : PluginDependency {
  override val isModule = true

  override fun createNewInstance(callback: PluginDependency.() -> Unit) = this.copy().apply(callback)

  override fun toString() = "$id (module, v2)"
}