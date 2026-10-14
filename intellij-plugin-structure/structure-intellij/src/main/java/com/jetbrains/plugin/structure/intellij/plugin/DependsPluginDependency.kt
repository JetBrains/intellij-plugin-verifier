/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.intellij.plugin.PluginCreator.Companion.v2ModulePrefix

/**
 * Legacy prefix for IntelliJ modules.
 * This convention was used for legacy `plugin.xml` files before Plugin Model v2.
 */
const val INTELLIJ_LEGACY_MODULES_PREFIX = "com.intellij.modules."

/**
 * Represents a `<depends>` element from the plugin.xml (v1-style dependency)
 */
class DependsPluginDependency(val pluginId: String, val isOptional: Boolean, val configFile: String? = null) {
  override fun toString(): String {
    return "Depends($pluginId" +
      if (isOptional) ", optional" else "" +
      if (configFile != null) ", configFile=$configFile" else "" +
      ")"
  }

  fun asPluginDependency() = if (isOptional) {
    PluginV1Dependency.Optional(pluginId)
  } else {
    PluginV1Dependency.Mandatory(pluginId)
  }

  fun resolveDescriptorPath(): String? {
    return if (isOptional && configFile != null) {
      if (v2ModulePrefix.matches(configFile)) "../${configFile}" else configFile
    } else {
      null
    }
  }

  /**
   * Indicates a legacy module dependency.
   * @see INTELLIJ_LEGACY_MODULES_PREFIX
   */
  fun isLegacyModule(): Boolean = this.pluginId.startsWith(INTELLIJ_LEGACY_MODULES_PREFIX)

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as DependsPluginDependency

    if (isOptional != other.isOptional) return false
    if (pluginId != other.pluginId) return false
    if (configFile != other.configFile) return false

    return true
  }

  override fun hashCode(): Int {
    var result = isOptional.hashCode()
    result = 31 * result + pluginId.hashCode()
    result = 31 * result + configFile.hashCode()
    return result
  }
}
