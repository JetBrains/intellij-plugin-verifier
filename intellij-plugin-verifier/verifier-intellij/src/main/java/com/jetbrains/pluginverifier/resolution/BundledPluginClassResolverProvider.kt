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
        .let { CompositeResolver.create(it) }
    }
  }