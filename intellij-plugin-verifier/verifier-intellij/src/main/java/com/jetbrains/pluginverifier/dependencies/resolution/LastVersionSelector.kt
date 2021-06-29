/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.dependencies.resolution

import com.jetbrains.plugin.structure.ide.VersionComparatorUtil
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.repositories.marketplace.MarketplaceRepository
import com.jetbrains.pluginverifier.repository.repositories.marketplace.UpdateInfo

/**
 * [PluginVersionSelector] that selects the _last_ version of the plugin from the repository.
 */
class LastVersionSelector : PluginVersionSelector {
  override fun selectPluginVersion(pluginId: String, pluginRepository: PluginRepository): PluginVersionSelector.Result {
    val allVersions = pluginRepository.getAllVersionsOfPlugin(pluginId)
    return selectLastVersion(allVersions, pluginRepository, "Plugin $pluginId is not found in $pluginRepository")
  }

  override fun selectPluginByModuleId(moduleId: String, pluginRepository: PluginRepository): PluginVersionSelector.Result {
    val plugins = pluginRepository.getPluginsDeclaringModule(moduleId, null)
    return selectLastVersion(plugins, pluginRepository, "Plugin declaring module '$moduleId' is not found in $pluginRepository")
  }

  private fun selectLastVersion(
    allVersions: List<PluginInfo>,
    pluginRepository: PluginRepository,
    notFoundMessage: String
  ): PluginVersionSelector.Result {
    val lastVersion = if (pluginRepository is MarketplaceRepository) {
      allVersions.maxByOrNull { it: PluginInfo -> (it as UpdateInfo).updateId }
    } else {
      allVersions.maxWithOrNull(compareBy(VersionComparatorUtil.COMPARATOR) { it.version })
    }
    return if (lastVersion != null) {
      PluginVersionSelector.Result.Selected(lastVersion)
    } else {
      PluginVersionSelector.Result.NotFound(notFoundMessage)
    }
  }
}