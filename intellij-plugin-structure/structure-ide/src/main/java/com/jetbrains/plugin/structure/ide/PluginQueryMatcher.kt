/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide

import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.PluginProvision
import com.jetbrains.plugin.structure.intellij.plugin.PluginProvision.Source.*
import com.jetbrains.plugin.structure.intellij.plugin.PluginQuery

class PluginQueryMatcher {
  fun matches(plugin: IdePlugin, query: PluginQuery): PluginProvision {
    val identifier = query.identifier
    return if (query.searchId() && plugin.pluginId == identifier) {
      PluginProvision.Found(plugin, ID)
    } else if (query.searchName() && plugin.pluginName == identifier) {
      PluginProvision.Found(plugin, NAME)
    } else if (query.searchContentModuleId() && plugin.contentModules.any { it.name == identifier }) {
      PluginProvision.Found(plugin, CONTENT_MODULE_ID)
    } else if (query.searchPluginAliases() && plugin.hasDefinedModuleWithId(identifier)) {
      PluginProvision.Found(plugin, ALIAS)
    } else PluginProvision.NotFound
  }
}