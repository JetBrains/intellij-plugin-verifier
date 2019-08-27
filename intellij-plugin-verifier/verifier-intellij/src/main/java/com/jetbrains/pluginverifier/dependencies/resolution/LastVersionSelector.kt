package com.jetbrains.pluginverifier.dependencies.resolution

import com.jetbrains.plugin.structure.ide.VersionComparatorUtil
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.repositories.marketplace.MarketplaceRepository
import com.jetbrains.pluginverifier.repository.repositories.marketplace.UpdateInfo

/**
 * [PluginVersionSelector] that selects the _last_ version of the plugin from the repository.
 */
class LastVersionSelector : PluginVersionSelector {
  override fun selectPluginVersion(pluginId: String, pluginRepository: PluginRepository): PluginVersionSelector.Result {
    val allVersionsOfPlugin = pluginRepository.getAllVersionsOfPlugin(pluginId)
    val lastVersion = if (pluginRepository is MarketplaceRepository) {
      allVersionsOfPlugin.maxBy { (it as UpdateInfo).updateId }
    } else {
      allVersionsOfPlugin.maxWith(compareBy(VersionComparatorUtil.COMPARATOR) { it.version })
    }
    return if (lastVersion == null) {
      PluginVersionSelector.Result.NotFound("Plugin $pluginId is not found in $pluginRepository")
    } else {
      PluginVersionSelector.Result.Selected(lastVersion)
    }
  }
}