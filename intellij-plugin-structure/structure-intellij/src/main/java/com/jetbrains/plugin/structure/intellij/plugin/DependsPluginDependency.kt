/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

/**
 * Represents a `<depends>` element from the plugin.xml
 */
class DependsPluginDependency(val pluginId: String, val isOptional: Boolean, val configFile: String? = null) {
  override fun toString(): String {
    return "Depends($pluginId" +
      if (isOptional) ", optional" else "" +
      if (configFile != null) ", configFile=$configFile" else "" +
      ")"
  }
}
