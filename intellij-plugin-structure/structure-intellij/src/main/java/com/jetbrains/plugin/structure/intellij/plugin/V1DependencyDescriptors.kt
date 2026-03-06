/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.intellij.beans.PluginDependencyBean
import com.jetbrains.plugin.structure.intellij.plugin.PluginCreator.Companion.v2ModulePrefix

internal class V1DependencyDescriptors {
  private val _descriptors: MutableMap<PluginDependency, String> = linkedMapOf()

  val descriptors: Map<PluginDependency, String> = _descriptors

  fun registerIfOptional(pluginDependency: PluginDependency, dependencyBean: PluginDependencyBean) {
    val descriptor = dependencyBean.configFile
    if (pluginDependency.isOptional && descriptor != null) {
      _descriptors[pluginDependency] =
        if (v2ModulePrefix.matches(descriptor)) "../${descriptor}" else descriptor
    }
  }
}