/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

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
    val allVersions = pluginRepository.getAllVersionsOfPlugin(pluginId)
    if (allVersions.isEmpty()) {
      return PluginVersionSelector.Result.NotFound("Plugin $pluginId is not available in ${pluginRepository.presentableName}")
    }
    return PluginVersionSelector.Result.NotFound("Plugin $pluginId doesn't have a build compatible with $ideVersion in ${pluginRepository.presentableName}")
  }
}