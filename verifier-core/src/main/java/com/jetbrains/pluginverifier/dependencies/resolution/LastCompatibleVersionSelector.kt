package com.jetbrains.pluginverifier.dependencies.resolution

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.UpdateInfo

/**
 * The [PluginVersionSelector] that selects
 * the _last_ version of the plugin _compatible_ with [ideVersion].
 */
class LastCompatibleVersionSelector(val ideVersion: IdeVersion) : PluginVersionSelector {
  override fun selectPluginVersion(pluginId: String, pluginRepository: PluginRepository): PluginVersionSelector.Result {
    val updateInfo = pluginRepository.getLastCompatibleVersionOfPlugin(ideVersion, pluginId) as? UpdateInfo
    if (updateInfo != null) {
      return PluginVersionSelector.Result.Plugin(updateInfo)
    }
    return PluginVersionSelector.Result.NotFound("Plugin $pluginId doesn't have a build compatible with ${ideVersion}")
  }
}