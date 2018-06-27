package com.jetbrains.pluginverifier.dependencies.resolution

import com.jetbrains.pluginverifier.misc.VersionComparatorUtil
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.repositories.marketplace.UpdateInfo

/**
 * [PluginVersionSelector] that selects the _last_ version
 * of the plugin from the [repository] [PluginRepository].
 */
class LastVersionSelector : PluginVersionSelector {
  override fun selectPluginVersion(pluginId: String, pluginRepository: PluginRepository): PluginVersionSelector.Result {
    val allVersionsOfPlugin = pluginRepository.getAllVersionsOfPlugin(pluginId)
    val lastVersion = allVersionsOfPlugin.maxWith(versionComparator)
    return if (lastVersion == null) {
      PluginVersionSelector.Result.NotFound("Plugin $pluginId is not found in the Plugin Repository")
    } else {
      PluginVersionSelector.Result.Selected(lastVersion)
    }
  }

  companion object {
    val versionComparator = Comparator<PluginInfo> { p1, p2 ->
      if (p1 is UpdateInfo && p2 is UpdateInfo) {
        Integer.compare(p1.updateId, p2.updateId)
      } else {
        VersionComparatorUtil.COMPARATOR.compare(p1.version, p2.version)
      }
    }
  }

}