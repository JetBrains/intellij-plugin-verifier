/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.resolution

import com.jetbrains.plugin.structure.base.utils.closeOnException
import com.jetbrains.plugin.structure.base.utils.rethrowIfInterrupted
import com.jetbrains.plugin.structure.classes.resolvers.CompositeResolver
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.ide.ProductInfoBasedIde
import com.jetbrains.plugin.structure.ide.classes.resolver.PluginDependencyFilteredResolver
import com.jetbrains.plugin.structure.ide.classes.resolver.ProductInfoClassResolver
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.pluginverifier.createPluginResolver
import com.jetbrains.pluginverifier.dependencies.DependenciesGraphBuilder
import com.jetbrains.pluginverifier.dependencies.resolution.DependencyFinder
import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.plugin.PluginDetails
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.repository.repositories.bundled.BundledPluginInfo
import com.jetbrains.pluginverifier.verifiers.packages.PackageFilter
import com.jetbrains.pluginverifier.verifiers.resolution.caching
import java.io.Closeable

class DefaultClassResolverProvider(
  private val dependencyFinder: DependencyFinder,
  private val ideDescriptor: IdeDescriptor,
  private val externalClassesPackageFilter: PackageFilter,
  private val additionalClassResolvers: List<Resolver> = emptyList()
) : ClassResolverProvider {

  private val bundledPluginClassResolverProvider = BundledPluginClassResolverProvider()

  override fun provide(checkedPluginDetails: PluginDetails): ClassResolverProvider.Result {
    val closeableResources = arrayListOf<Closeable>()
    closeableResources.closeOnException {
      val pluginResolver = checkedPluginDetails.pluginClassesLocations.createPluginResolver()

      val (dependenciesGraph, dependenciesResults) =
        DependenciesGraphBuilder(dependencyFinder).buildDependenciesGraph(checkedPluginDetails.idePlugin, ideDescriptor.ide)

      closeableResources += dependenciesResults

      val dependenciesClassResolver = createDependenciesClassResolver(checkedPluginDetails, dependenciesResults)

      val resolvers = listOf(
        pluginResolver,
        ideDescriptor.jdkDescriptor.jdkResolver,
        getIdeResolver(checkedPluginDetails.idePlugin, ideDescriptor),
        dependenciesClassResolver
      ) + additionalClassResolvers

      val resolver = CompositeResolver.create(resolvers).caching()
      return ClassResolverProvider.Result(pluginResolver, resolver, dependenciesGraph, closeableResources)
    }
  }

  override fun provideExternalClassesPackageFilter() = externalClassesPackageFilter

  private fun getIdeResolver(plugin: IdePlugin, ideDescriptor: IdeDescriptor): Resolver {
    return if (ideDescriptor.ide is ProductInfoBasedIde && ideDescriptor.ideResolver is ProductInfoClassResolver) {
      PluginDependencyFilteredResolver(plugin, ideDescriptor.ideResolver)
    } else {
      ideDescriptor.ideResolver
    }
  }

  private fun createPluginResolver(pluginDependency: PluginDetails): Resolver =
    when (pluginDependency.pluginInfo) {
      is BundledPluginInfo -> bundledPluginClassResolverProvider.getResolver(pluginDependency)
      else -> pluginDependency.pluginClassesLocations.createPluginResolver()
    }

  private fun createDependenciesClassResolver(checkedPluginDetails: PluginDetails, dependencies: List<DependencyFinder.Result>): Resolver {
    val resolvers = mutableListOf<Resolver>()
    resolvers.closeOnException {
      val pluginDetails = dependencies
        .filterIsInstance<DependencyFinder.Result.DetailsProvided>()
        .map { it.pluginDetailsCacheResult }
        .filterIsInstance<PluginDetailsCache.Result.Provided>()
        .map { it.pluginDetails }

      resolvers += pluginDetails.mapNotNullInterruptible { createPluginResolver(it) }
    }
    return CompositeResolver.create(resolvers)
  }

  private inline fun <T, R> Iterable<T>.mapNotNullInterruptible(transform: (T) -> R): List<R> {
    return mapNotNull {
      try {
        transform(it)
      } catch (e: Exception) {
        e.rethrowIfInterrupted()
        null
      }
    }
  }
}