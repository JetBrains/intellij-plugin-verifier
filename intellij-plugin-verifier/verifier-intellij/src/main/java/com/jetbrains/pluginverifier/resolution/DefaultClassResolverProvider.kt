package com.jetbrains.pluginverifier.resolution

import com.jetbrains.plugin.structure.base.utils.closeOnException
import com.jetbrains.plugin.structure.base.utils.rethrowIfInterrupted
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.classes.resolvers.UnionResolver
import com.jetbrains.plugin.structure.ide.util.KnownIdePackages
import com.jetbrains.pluginverifier.createPluginResolver
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.dependencies.DependenciesGraphBuilder
import com.jetbrains.pluginverifier.dependencies.resolution.DependencyFinder
import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.parameters.jdk.JdkDescriptorsCache
import com.jetbrains.pluginverifier.parameters.packages.PackageFilter
import com.jetbrains.pluginverifier.plugin.PluginDetails
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.reporting.verification.Reporters
import com.jetbrains.pluginverifier.repository.cache.ResourceCacheEntryResult
import com.jetbrains.pluginverifier.results.VerificationResult
import com.jetbrains.pluginverifier.results.structure.PluginStructureWarning
import com.jetbrains.pluginverifier.verifiers.resolution.ClassResolver
import com.jetbrains.pluginverifier.verifiers.resolution.IdePluginClassResolver
import java.io.Closeable
import java.nio.file.Path

/**
 * [ClassResolverProvider] that provides the [IdePluginClassResolver].
 */
class DefaultClassResolverProvider(
    private val dependencyFinder: DependencyFinder,
    private val jdkDescriptorsCache: JdkDescriptorsCache,
    private val jdkPath: Path,
    private val ideDescriptor: IdeDescriptor,
    private val externalClassesPackageFilter: PackageFilter
) : ClassResolverProvider {

  override fun provide(
      checkedPluginDetails: PluginDetails,
      verificationResult: VerificationResult,
      pluginReporters: Reporters
  ): ClassResolver {
    val pluginResolver = checkedPluginDetails.pluginClassesLocations.createPluginResolver()
    findMistakenlyBundledIdeClasses(pluginResolver, verificationResult)

    val (dependenciesGraph, dependenciesResults) =
        DependenciesGraphBuilder(dependencyFinder).buildDependenciesGraph(checkedPluginDetails.idePlugin, ideDescriptor.ide)

    verificationResult.dependenciesGraph = dependenciesGraph
    verificationResult.addDependenciesWarnings(dependenciesGraph)

    return createClassResolver(pluginResolver, dependenciesResults)
  }

  private fun VerificationResult.addDependenciesWarnings(dependenciesGraph: DependenciesGraph) {
    val cycles = dependenciesGraph.getAllCycles()
    for (cycle in cycles) {
      pluginStructureWarnings += PluginStructureWarning(
          "The plugin is on a dependencies cycle: " + cycle.joinToString(separator = " -> ") + " -> " + cycle[0]
      )
    }
  }

  private fun findMistakenlyBundledIdeClasses(pluginResolver: Resolver, resultHolder: VerificationResult) {
    val idePackages = pluginResolver.allPackages.filter { KnownIdePackages.isKnownPackage(it) }
    val message = buildString {
      append("The plugin distribution contains IDE packages: ")
      if (idePackages.size < 5) {
        append(idePackages.joinToString())
      } else {
        append(idePackages.take(3).joinToString())
        append(" and ${idePackages.size - 3} other")
      }
      append(". ")
      append("Bundling IDE classes is considered bad practice and may lead to sophisticated compatibility problems. ")
      append("Consider excluding IDE classes from the plugin distribution and reusing the IDE's classes. ")
      append("If your plugin depends on classes of an IDE bundled plugin, explicitly specify dependency on that plugin instead of bundling it. ")
    }
    if (idePackages.isNotEmpty()) {
      resultHolder.pluginStructureWarnings += PluginStructureWarning(message)
    }
  }

  private fun createClassResolver(
      pluginResolver: Resolver,
      dependenciesResults: List<DependencyFinder.Result>
  ): ClassResolver {
    //Do not close the pluginResolver. It will be closed along with checkedPluginDetails.

    val closeableResources = arrayListOf<Closeable>()
    closeableResources.closeOnException {
      closeableResources += dependenciesResults

      val dependenciesResolver = createDependenciesResolver(dependenciesResults)
      return when (val jdkCacheEntry = jdkDescriptorsCache.getJdkResolver(jdkPath)) {
        is ResourceCacheEntryResult.Found -> {
          closeableResources += jdkCacheEntry.resourceCacheEntry

          val jdkClassesResolver = jdkCacheEntry.resourceCacheEntry.resource.jdkResolver

          IdePluginClassResolver(
              pluginResolver,
              dependenciesResolver,
              jdkClassesResolver,
              ideDescriptor.ideResolver,
              externalClassesPackageFilter,
              closeableResources
          )
        }
        is ResourceCacheEntryResult.Failed -> throw IllegalStateException("Unable to resolve JDK descriptor", jdkCacheEntry.error)
        is ResourceCacheEntryResult.NotFound -> throw IllegalStateException("Unable to find JDK $jdkPath: ${jdkCacheEntry.message}")
      }
    }
  }

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
    }
    return UnionResolver.create(resolvers)
  }
}