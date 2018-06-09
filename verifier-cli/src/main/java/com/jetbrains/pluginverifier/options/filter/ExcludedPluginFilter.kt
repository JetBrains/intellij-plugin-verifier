package com.jetbrains.pluginverifier.options.filter

import com.jetbrains.pluginverifier.repository.PluginIdAndVersion
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