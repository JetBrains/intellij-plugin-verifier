package com.jetbrains.pluginverifier.resolution

import com.jetbrains.plugin.structure.base.utils.closeOnException
import com.jetbrains.plugin.structure.base.utils.rethrowIfInterrupted
import com.jetbrains.plugin.structure.classes.resolvers.CompositeResolver
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.pluginverifier.createPluginResolver
import com.jetbrains.pluginverifier.dependencies.DependenciesGraphBuilder
import com.jetbrains.pluginverifier.dependencies.resolution.DependencyFinder
import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.jdk.JdkDescriptorsCache
import com.jetbrains.pluginverifier.plugin.PluginDetails
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.repository.cache.ResourceCacheEntryResult
import com.jetbrains.pluginverifier.verifiers.packages.PackageFilter
import com.jetbrains.pluginverifier.verifiers.resolution.caching
import java.io.Closeable
import java.nio.file.Path

class DefaultClassResolverProvider(
    private val dependencyFinder: DependencyFinder,
    private val jdkDescriptorsCache: JdkDescriptorsCache,
    private val jdkPath: Path,
    private val ideDescriptor: IdeDescriptor,
    private val externalClassesPackageFilter: PackageFilter
) : ClassResolverProvider {

  override fun provide(checkedPluginDetails: PluginDetails): ClassResolverProvider.Result {
    val closeableResources = arrayListOf<Closeable>()
    closeableResources.closeOnException {
      val pluginResolver = checkedPluginDetails.pluginClassesLocations.createPluginResolver()

      val (dependenciesGraph, dependenciesResults) =
          DependenciesGraphBuilder(dependencyFinder).buildDependenciesGraph(checkedPluginDetails.idePlugin, ideDescriptor.ide)

      closeableResources += dependenciesResults

      val ideClassResolver = ideDescriptor.ideResolver
      val dependenciesClassResolver = createDependenciesResolver(dependenciesResults)
      return when (val jdkCacheEntry = jdkDescriptorsCache.getJdkResolver(jdkPath)) {
        is ResourceCacheEntryResult.Found -> {
          closeableResources += jdkCacheEntry.resourceCacheEntry

          val jdkClassResolver = jdkCacheEntry.resourceCacheEntry.resource.jdkResolver

          val resolver = CompositeResolver.create(pluginResolver, jdkClassResolver, ideClassResolver, dependenciesClassResolver).caching()

          ClassResolverProvider.Result(pluginResolver, resolver, dependenciesGraph, closeableResources)
        }
        is ResourceCacheEntryResult.Failed -> throw IllegalStateException("Unable to resolve JDK descriptor", jdkCacheEntry.error)
        is ResourceCacheEntryResult.NotFound -> throw IllegalStateException("Unable to find JDK $jdkPath: ${jdkCacheEntry.message}")
      }
    }
  }

  override fun provideExternalClassesPackageFilter() = externalClassesPackageFilter

  private fun createDependenciesResolver(results: List<DependencyFinder.Result>): Resolver {
    val resolvers = arrayListOf<Resolver>()
    resolvers.closeOnException {
      for (result in results) {
        if (result is DependencyFinder.Result.DetailsProvided) {
          val cacheResult = result.pluginDetailsCacheResult
          if (cacheResult is PluginDetailsCache.Result.Provided) {
            val resolver = try {
              cacheResult.pluginDetails.pluginClassesLocations.createPluginResolver()
            } catch (e: Exception) {
              e.rethrowIfInterrupted()
              continue
            }
            resolvers.add(resolver)
          }
        }
      }
      return CompositeResolver.create(resolvers)
    }
  }
}