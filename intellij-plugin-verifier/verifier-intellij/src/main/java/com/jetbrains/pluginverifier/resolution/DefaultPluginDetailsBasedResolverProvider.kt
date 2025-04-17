/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.resolution

import com.jetbrains.pluginverifier.createPluginResolver
import com.jetbrains.pluginverifier.plugin.PluginDetails

class DefaultPluginDetailsBasedResolverProvider : PluginDetailsBasedResolverProvider {
  override fun getPluginResolver(pluginDependency: PluginDetails) =
    pluginDependency.pluginClassesLocations.createPluginResolver(pluginDependency.pluginInfo.pluginId)
}