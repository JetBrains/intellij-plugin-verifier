/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.options.filter

import com.jetbrains.plugin.structure.ide.PluginIdAndVersion
import com.jetbrains.pluginverifier.repository.PluginInfo

/**
 * [PluginFilter] that verifies only plugins
 * that are not present in the [excludedPlugins].
 */
class ExcludedPluginFilter(private val excludedPlugins: Set<PluginIdAndVersion>) : PluginFilter {

  override fun shouldVerifyPlugin(pluginInfo: PluginInfo): PluginFilter.Result {
    if (PluginIdAndVersion(pluginInfo.pluginId, pluginInfo.version) in excludedPlugins) {
      return PluginFilter.Result.Ignore("The plugin $pluginInfo is listed in the excluded plugins list")
    }
    return PluginFilter.Result.Verify
  }

}