/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.dependencies.resolution

import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.PluginRepository

/**
 * [Selects] [selectPluginVersion] a specific [version] [PluginInfo]
 * of the plugin among all plugins available in the [PluginRepository].
 */
interface PluginVersionSelector {

  fun selectPluginVersion(pluginId: String, pluginRepository: PluginRepository): Result

  sealed class Result {
    /**
     * Successfully selected one version of the plugin - [pluginInfo].
     */
    data class Selected(val pluginInfo: PluginInfo) : Result()

    /**
     * Plugin is not found in the plugin repository.
     */
    data class NotFound(val reason: String) : Result()
  }

}