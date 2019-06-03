package com.jetbrains.pluginverifier.resolution

import com.jetbrains.plugin.structure.base.utils.closeLogged
import com.jetbrains.plugin.structure.base.utils.closeOnException
import com.jetbrains.plugin.structure.base.utils.rethrowIfInterrupted
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.classes.resolvers.UnionResolver
import com.jetbrains.plugin.structure.ide.util.KnownIdePackages
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependencyImpl
import com.jetbrains.pluginverifier.createPluginResolver
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.dependencies.graph.DepEdge
import com.jetbrains.pluginverifier.dependencies.graph.DepGraph2ApiGraphConverter
import com.jetbrains.pluginverifier.dependencies.graph.DepGraphBuilder
import com.jetbrains.pluginverifier.dependencies.graph.DepVertex
import com.jetbrains.pluginverifier.dependencies.resolution.DependencyFinder
import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.parameters.jdk.JdkDescriptorsCache
import com.jetbrains.pluginverifier.parameters.jdk.JdkPath
import com.jetbrains.pluginverifier.parameters.packages.PackageFilter
import com.jetbrains.pluginverifier.plugin.PluginDetails
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.reporting.verification.Reporters
import com.jetbrains.pluginverifier.repository.cache.ResourceCacheEntryResult
import com.jetbrains.pluginverifier.results.VerificationResult
import com.jetbrains.pluginverifier.results.structure.PluginStructureWarning
import com.jetbrains.pluginverifier.verifiers.resolution.ClassResolver
import com.jetbrains.pluginverifier.verifiers.resolution.IdePluginClassResolver
import org.jgrapht.DirectedGraph
import org.jgrapht.graph.DefaultDirectedGraph
import java.io.Closeable

/**
 * [ClassResolverProvider] that provides the [IdePluginClassResolver].
 */
class DefaultClassResolverProvider(
    private val dependencyFinder: DependencyFinder,
    private val jdkDescriptorsCache: JdkDescriptorsCache,
    private val jdkPath: JdkPath,
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

    val depGraph = DefaultDirectedGraph<DepVertex, DepEdge>(DepEdge::class.java)
    try {
      val apiGraph = buildDependenciesGraph(checkedPluginDetails.idePlugin, depGraph)
      verificationResult.dependenciesGraph = apiGraph
      verificationResult.addDependenciesWarnings(apiGraph)
      return createClassResolver(pluginResolver, depGraph)
    } catch (e: Throwable) {
      depGraph.vertexSet().forEach { it.dependencyResult.closeLogged() }
      throw e
    }
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
      depGraph: DirectedGraph<DepVertex, DepEdge>
  ): ClassResolver {
    val dependenciesResults = depGraph.vertexSet().map { it.dependencyResult }
    val dependenciesResolver = createDependenciesResolver(depGraph)

    return when (val jdkCacheEntry = jdkDescriptorsCache.getJdkResolver(jdkPath)) {
      is ResourceCacheEntryResult.Found -> {
        val jdkClassesResolver = jdkCacheEntry.resourceCacheEntry.resource.jdkClassesResolver
        val closeableResources = listOf<Closeable>(jdkCacheEntry.resourceCacheEntry) + dependenciesResults
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

  private fun buildDependenciesGraph(
      plugin: IdePlugin,
      dependenciesGraph: DirectedGraph<DepVertex, DepEdge>
  ): DependenciesGraph {
    val start = DepVertex(plugin.pluginId!!, DependencyFinder.Result.FoundPlugin(plugin))
    val depGraphBuilder = DepGraphBuilder(dependencyFinder)
    depGraphBuilder.addTransitiveDependencies(dependenciesGraph, start)
    maybeAddOptionalJavaPluginDependency(plugin, depGraphBuilder, dependenciesGraph)
    maybeAddBundledPluginsWithUseIdeaClassLoader(depGraphBuilder, dependenciesGraph)
    return DepGraph2ApiGraphConverter(ideDescriptor.ideVersion).convert(dependenciesGraph, start)
  }

  /**
   * If a plugin does not include any module dependency tags in its plugin.xml,
   * it is assumed to be a legacy plugin and is loaded only in IntelliJ IDEA
   * https://www.jetbrains.org/intellij/sdk/docs/basics/getting_started/plugin_compatibility.html
   *
   * But since we've recently extracted Java to a separate plugin, many plugins may stop working
   * because they depend on Java plugin classes but do not explicitly declare a dependency onto 'com.intellij.modules.java'.
   *
   * So let's forcibly add Java as an optional dependency for such plugins.
   */
  private fun maybeAddOptionalJavaPluginDependency(
      plugin: IdePlugin,
      depGraphBuilder: DepGraphBuilder,
      dependenciesGraph: DirectedGraph<DepVertex, DepEdge>
  ) {
    if (plugin.dependencies.none { it.isModule }) {
      val javaModuleDependency = PluginDependencyImpl("com.intellij.modules.java", true, true)
      val dependencyResult = dependencyFinder.findPluginDependency(javaModuleDependency)
      val javaPluginVertex = DepVertex("com.intellij.java", dependencyResult)
      depGraphBuilder.addTransitiveDependencies(dependenciesGraph, javaPluginVertex)
    }
  }

  /**
   * Bundled plugins that specify `<idea-plugin use-idea-classloader="true">` are automatically added to
   * platform class loader and may be referenced by other plugins without explicit dependency on them.
   *
   * We would like to emulate this behaviour by forcibly adding such plugins to the verification classpath.
   */
  private fun maybeAddBundledPluginsWithUseIdeaClassLoader(
      depGraphBuilder: DepGraphBuilder,
      dependenciesGraph: DirectedGraph<DepVertex, DepEdge>
  ) {
    for (bundledPlugin in ideDescriptor.ide.bundledPlugins) {
      if (bundledPlugin.useIdeClassLoader) {
        val dependencyResult = DependencyFinder.Result.FoundPlugin(bundledPlugin)
        val dependencyId = (bundledPlugin.pluginId ?: bundledPlugin.pluginName)!!
        val bundledVertex = DepVertex(dependencyId, dependencyResult)
        depGraphBuilder.addTransitiveDependencies(dependenciesGraph, bundledVertex)
      }
    }
  }

  private fun createDependenciesResolver(graph: DirectedGraph<DepVertex, DepEdge>): Resolver {
    val dependenciesResolvers = arrayListOf<Resolver>()
    dependenciesResolvers.closeOnException {
      for (depVertex in graph.vertexSet()) {
        val result = depVertex.dependencyResult
        if (result is DependencyFinder.Result.DetailsProvided) {
          val cacheResult = result.pluginDetailsCacheResult
          if (cacheResult is PluginDetailsCache.Result.Provided) {
            val pluginResolver = try {
              cacheResult.pluginDetails.pluginClassesLocations.createPluginResolver()
            } catch (e: Exception) {
              e.rethrowIfInterrupted()
              continue
            }
            dependenciesResolvers.add(pluginResolver)
          }
        }
      }
      return UnionResolver.create(dependenciesResolvers)
    }
  }
}