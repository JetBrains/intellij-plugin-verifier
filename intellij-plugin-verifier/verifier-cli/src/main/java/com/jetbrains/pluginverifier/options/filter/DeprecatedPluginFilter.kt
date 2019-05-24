package com.jetbrains.pluginverifier.options.filter

import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.repositories.marketplace.UpdateInfo

/**
 * [PluginFilter] that excludes from verification plugins
 * that are marked with "deprecated" tag in the Plugin Repository.
 *
 * Plugins marked with such tags are not maintained by their authors anymore,
 * and it doesn't make sense to report API incompatibilities with these plugins.
 */
class DeprecatedPluginFilter : PluginFilter {

  companion object {
    val deprecatedTags = System.getProperty("plugin.verifier.deprecated.plugins.tags", "deprecated").split(";").toSet()
  }

  override fun shouldVerifyPlugin(pluginInfo: PluginInfo): PluginFilter.Result {
    if (pluginInfo is UpdateInfo) {
      val deprecatedTag = pluginInfo.tags.find { it in deprecatedTags }
      if (deprecatedTag != null) {
        return PluginFilter.Result.Ignore("The plugin $pluginInfo is marked with tag '$deprecatedTag'.")
      }
    }
    return PluginFilter.Result.Verify
  }
}