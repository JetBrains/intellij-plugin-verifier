/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.resolution

import com.jetbrains.plugin.structure.classes.resolvers.CompositeResolver
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.pluginverifier.filtering.ExternalBuildClassesSelector
import com.jetbrains.pluginverifier.filtering.MainClassesSelector
import com.jetbrains.pluginverifier.plugin.PluginDetails

class BundledPluginClassResolverProvider {
  private val bundledClassesSelectors = listOf(MainClassesSelector.forBundledPlugin(), ExternalBuildClassesSelector())

  fun getResolver(pluginDetails: PluginDetails): Resolver {
    val classLocations = pluginDetails.pluginClassesLocations
    return bundledClassesSelectors.flatMap { it.getClassLoader(classLocations) }
      .let { CompositeResolver.create(it, pluginDetails.pluginInfo.pluginId) }
  }
}