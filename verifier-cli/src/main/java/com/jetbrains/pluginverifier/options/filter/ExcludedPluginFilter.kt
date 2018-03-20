package com.jetbrains.pluginverifier.options.filter

import com.jetbrains.pluginverifier.parameters.filtering.PluginIdAndVersion
import com.jetbrains.pluginverifier.parameters.filtering.toPluginIdAndVersion
import com.jetbrains.pluginverifier.repository.PluginInfo

/**
 * [PluginFilter] that verifies only plugins
 * that are not present in the [excludedPlugins].
 */
class ExcludedPluginFilter(private val excludedPlugins: Set<PluginIdAndVersion>) : PluginFilter {

  override fun shouldVerifyPlugin(pluginInfo: PluginInfo): PluginFilter.Result {
    if (pluginInfo.toPluginIdAndVersion() in excludedPlugins) {
      return PluginFilter.Result.Ignore("The plugin $pluginInfo is listed in the excluded plugins list")
    }
    return PluginFilter.Result.Verify
  }

}