package com.jetbrains.pluginverifier.dependencies.resolution

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.repository.PluginRepository

/**
 * The [PluginVersionSelector] that selects
 * the _last_ version of the plugin _compatible_ with [ideVersion].
 */
class LastCompatibleVersionSelector(val ideVersion: IdeVersion) : PluginVersionSelector {
  override fun selectPluginVersion(pluginId: String, pluginRepository: PluginRepository): PluginVersionSelector.Result {
    val pluginInfo = pluginRepository.getLastCompatibleVersionOfPlugin(ideVersion, pluginId)
    if (pluginInfo != null) {
      return PluginVersionSelector.Result.Selected(pluginInfo)
    }
    return PluginVersionSelector.Result.NotFound("Plugin $pluginId doesn't have a build compatible with $ideVersion")
  }
}