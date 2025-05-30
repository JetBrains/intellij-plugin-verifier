/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.resolution

import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.base.utils.closeOnException
import com.jetbrains.plugin.structure.base.utils.rethrowIfInterrupted
import com.jetbrains.plugin.structure.classes.resolvers.CompositeResolver
import com.jetbrains.plugin.structure.classes.resolvers.LazyCompositeResolver
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.ide.ProductInfoAware
import com.jetbrains.plugin.structure.ide.classes.resolver.CachingPluginDependencyResolverProvider
import com.jetbrains.plugin.structure.ide.classes.resolver.CachingPluginDependencyResolverProvider.DependencyTreeAwareResolver
import com.jetbrains.plugin.structure.ide.classes.resolver.ProductInfoClassResolver
import com.jetbrains.plugin.structure.intellij.classes.locator.CompileServerExtensionKey
import com.jetbrains.plugin.structure.intellij.classes.plugin.BundledPluginClassesFinder
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.LegacyPluginAnalysis
import com.jetbrains.plugin.structure.intellij.plugin.StructurallyValidated
import com.jetbrains.pluginverifier.createPluginResolver
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.dependencies.DependenciesGraphBuilder
import com.jetbrains.pluginverifier.dependencies.DependenciesGraphProvider
import com.jetbrains.pluginverifier.dependencies.resolution.DependencyFinder
import com.jetbrains.pluginverifier.dependencies.resolution.DependencyOrigin.Bundled
import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.plugin.PluginDetails
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.repository.repositories.bundled.BundledPluginInfo
import com.jetbrains.pluginverifier.repository.repositories.dependency.DependencyPluginInfo
import com.jetbrains.pluginverifier.repository.repositories.local.LocalPluginInfo
import com.jetbrains.pluginverifier.verifiers.packages.PackageFilter
import com.jetbrains.pluginverifier.verifiers.resolution.caching
import java.io.Closeable

class DefaultClassResolverProvider(
  private val dependencyFinder: DependencyFinder,
  private val ideDescriptor: IdeDescriptor,
  private val externalClassesPackageFilter: PackageFilter,
  private val additionalClassResolvers: List<Resolver> = emptyList(),
  private val pluginDetailsBasedResolverProvider: PluginDetailsBasedResolverProvider = DefaultPluginDetailsBasedResolverProvider(),
  private val downloadUnavailableBundledPlugins: Boolean = false
) : ClassResolverProvider {

  private val secondaryResolver = if (ideDescriptor.ideResolver is ProductInfoClassResolver) ideDescriptor.ideResolver else null

  private val pluginResolverProvider = CachingPluginDependencyResolverProvider(ideDescriptor.ide, secondaryResolver)

  private val bundledPluginClassResolverProvider = BundledPluginClassResolverProvider()

  private val legacyPluginAnalysis = LegacyPluginAnalysis()

  private val dependenciesGraphProvider = DependenciesGraphProvider()

  override fun provide(checkedPluginDetails: PluginDetails): ClassResolverProvider.Result {
    val closeableResources = arrayListOf<Closeable>()
    closeableResources.closeOnException {
      val pluginResolver = checkedPluginDetails.pluginClassesLocations.createPluginResolver(checkedPluginDetails.pluginInfo.pluginId)

      // this fills the `pluginResolverProviderCache`
      val ideResolver = getIdeResolver(checkedPluginDetails.idePlugin, ideDescriptor)
      val allResolvers = mutableListOf<Resolver>()
      allResolvers += pluginResolver.also { closeableResources += it }
      allResolvers += ideDescriptor.jdkDescriptor.jdkResolver
      allResolvers += ideResolver

      val dependenciesGraph: DependenciesGraph
      if (downloadUnavailableBundledPlugins
        || !ideDescriptor.isProductInfoBased()
        || legacyPluginAnalysis.isLegacyPlugin(checkedPluginDetails.idePlugin)
        || ideResolver !is DependencyTreeAwareResolver
        ) {
        val (depGraph, dependenciesResults) =
          DependenciesGraphBuilder(dependencyFinder).buildDependenciesGraph(checkedPluginDetails.idePlugin, ideDescriptor.ide)
        closeableResources += dependenciesResults

        // Resolve dependencies via DependencyFinder mechanism.
        // For 'product-info.json'-based IDEs, the 'ideResolver' already contains
        // only overlap between plugin dependencies and IDE bundled plugins.
        createDependenciesClassResolver(checkedPluginDetails, dependenciesResults).also {
          allResolvers += it
        }
        dependenciesGraph = depGraph
      } else {
        val dependencyTreeResolution = (ideResolver as DependencyTreeAwareResolver).dependencyTreeResolution
        dependenciesGraph = dependenciesGraphProvider.getDependenciesGraph(dependencyTreeResolution)
      }

      allResolvers += additionalClassResolvers

      val resolver = LazyCompositeResolver.create(allResolvers, checkedPluginDetails.pluginInfo.pluginId).caching()
      return ClassResolverProvider.Result(pluginResolver, resolver, dependenciesGraph, closeableResources)
    }
  }

  override fun provideExternalClassesPackageFilter() = externalClassesPackageFilter

  private fun getIdeResolver(plugin: IdePlugin, ideDescriptor: IdeDescriptor): Resolver {
    return if (ideDescriptor.isProductInfoBased()
      && !legacyPluginAnalysis.isLegacyPlugin(plugin)
    ) {
      pluginResolverProvider.getResolver(plugin)
    } else {
      ideDescriptor.ideResolver
    }
  }

  private fun IdeDescriptor.isProductInfoBased(): Boolean {
    return ide is ProductInfoAware && ideResolver is ProductInfoClassResolver
  }

  private fun createPluginResolver(pluginDependency: PluginDetails): Resolver = with(pluginDependency.pluginInfo) {
    // reuse cached resolvers from IDE
    if (pluginResolverProvider.contains(pluginId)) {
      return pluginResolverProvider.getResolver(pluginDependency.idePlugin)
    }

    return when (this) {
      is BundledPluginInfo -> createBundledPluginResolver(pluginDependency)
        ?: bundledPluginClassResolverProvider.getResolver(pluginDependency)

      else -> pluginDetailsBasedResolverProvider.getPluginResolver(pluginDependency)
    }
  }

  private fun createBundledPluginResolver(pluginDependency: PluginDetails): Resolver? {
    return if (ideDescriptor.ide is ProductInfoAware && ideDescriptor.ideResolver is ProductInfoClassResolver) {
      ideDescriptor.ideResolver.getLayoutComponentResolver(pluginDependency.pluginInfo.pluginId)
    } else null
  }

  private fun createDependenciesClassResolver(checkedPluginDetails: PluginDetails, dependencies: List<DependencyFinder.Result>): Resolver {
    val resolvers = mutableListOf<Resolver>()
    resolvers += createResolvers(dependencies)

    val pluginId = checkedPluginDetails.pluginInfo.pluginId
    return CompositeResolver.create(resolvers, resolverName = "Plugin Dependency Composite Resolver for '$pluginId'")
  }

  private fun createResolvers(dependencies: List<DependencyFinder.Result>): List<Resolver> {
    val resolvers = mutableListOf<Resolver>()
    resolvers.closeOnException {
      val pluginDetails = dependencies.mapNotNull {
        when (it) {
          is DependencyFinder.Result.DetailsProvided -> it.getDetails()
          is DependencyFinder.Result.FoundPlugin -> it.getDetails()
          else -> null
        }
      }

      resolvers += pluginDetails.mapNotNullInterruptible { createPluginResolver(it) }
    }
    return resolvers
  }

  private fun getNonBundledDependencyDetails(plugin: IdePlugin): PluginDetails {
    val pluginWarnings =
      (if (plugin is StructurallyValidated) plugin.problems else emptyList()).filter { it.level == PluginProblem.Level.WARNING }
    return PluginDetails(
      DependencyPluginInfo(LocalPluginInfo(plugin)), plugin, pluginWarnings,
      BundledPluginClassesFinder.findPluginClasses(plugin, additionalKeys = listOf(CompileServerExtensionKey)), null
    )
  }

  private fun getBundledPluginDetails(ide: Ide, plugin: IdePlugin): PluginDetails {
    val pluginWarnings =
      (if (plugin is StructurallyValidated) plugin.problems else emptyList()).filter { it.level == PluginProblem.Level.WARNING }
    return PluginDetails(
      BundledPluginInfo(ide.version, plugin), plugin, pluginWarnings,
      BundledPluginClassesFinder.findPluginClasses(plugin, additionalKeys = listOf(CompileServerExtensionKey)), null
    )
  }

  private fun DependencyFinder.Result.FoundPlugin.getDetails(): PluginDetails {
    return if (origin == Bundled) {
      getBundledPluginDetails(ideDescriptor.ide, plugin)
    } else {
      getNonBundledDependencyDetails(plugin)
    }
  }

  private fun DependencyFinder.Result.DetailsProvided.getDetails(): PluginDetails? {
    return (pluginDetailsCacheResult as? PluginDetailsCache.Result.Provided)?.pluginDetails
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