/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

/**
 * Represents a mandatory dependency on a module in a Plugin V2 model.
 * This maps to the `<dependencies>/<module>` element in the plugin descriptor.
 */
data class ModuleV2Dependency(override val id: String, override var isOptional: Boolean = false) : PluginDependency {
  override val isModule = true

  /**
   * Returns a copy of this dependency with [isOptional] flag set to true.
   * Note that this is an artificial case, as per Plugin V2 model, the module dependency
   * is always mandatory and cannot be optional.
   */
  override fun asOptional(): ModuleV2Dependency = copy(isOptional = true)

  override fun toString() = "$id (module, v2)"
}