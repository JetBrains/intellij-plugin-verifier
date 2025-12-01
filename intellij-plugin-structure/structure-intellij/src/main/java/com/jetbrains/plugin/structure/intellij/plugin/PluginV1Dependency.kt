/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

/**
 * Represents a Plugin Model v1 Dependency declared in `<depends>` element.
 * @param id id of the plugin that is a dependency
 * @param isOptional indicates that the `optional` attribute was set to true and an optional descriptor file has been provided.
 */
sealed class PluginV1Dependency(override val id: String, override val isOptional: Boolean = false) : PluginDependency {
  override val isModule = false

  /**
   * Represents a mandatory Plugin Model v1 Dependency declared in `<depends>` element.
   * @param id id of the plugin that is a dependency
   */
  data class Mandatory(override val id: String) : PluginV1Dependency(id) {
    override fun asOptional(): PluginV1Dependency = Optional(id)

    override fun toString() = "$id (v1)"
  }

  /**
   * Represents an optional Plugin Model v1 Dependency declared in `<depends>` element
   * along with a configuration file.
   * @param id id of the plugin that is a dependency
   */
  data class Optional(override val id: String) : PluginV1Dependency(id, isOptional = true) {
    override fun asOptional(): Optional = this

    override fun toString() = "$id (optional, v1)"
  }
}