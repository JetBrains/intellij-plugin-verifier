/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.resolution

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.pluginverifier.plugin.PluginDetails

fun interface PluginDetailsBasedResolverProvider {
  fun getPluginResolver(pluginDependency: PluginDetails): Resolver
}