/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

/**
 * Represents a dependency on the implicit main module of a plugin (`<dependencies><plugin id="..."/>` statement).
 */
class PluginMainModuleDependency(val pluginId: String) {
  override fun toString(): String = "PluginMainModuleDependency(pluginId='$pluginId')"
}
