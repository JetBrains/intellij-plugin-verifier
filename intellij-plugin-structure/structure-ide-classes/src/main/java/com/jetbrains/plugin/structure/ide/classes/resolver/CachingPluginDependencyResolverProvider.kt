/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide.classes.resolver

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.stats.CacheStats
import com.jetbrains.plugin.structure.classes.resolvers.*
import com.jetbrains.plugin.structure.classes.resolvers.Resolver.ReadMode
import com.jetbrains.plugin.structure.ide.classes.IdeFileOrigin
import com.jetbrains.plugin.structure.intellij.plugin.*
import com.jetbrains.plugin.structure.intellij.plugin.dependencies.*

/**
 * See also cache size in [CacheResolver].
 */
private const val DEFAULT_CACHE_SIZE = 1024L

private const val UNKNOWN_DEPENDENCY_ID = "Unknown ID"

private data class PluginArtifactKey(
  val id: String,
  val version: String?,
  val originalFile: java.nio.file.Path?
)

private data class PluginResolverKey(
  val plugin: PluginArtifactKey,
  val resolverName: String
)

class CachingPluginDependencyResolverProvider(
  pluginProvider: PluginProvider,
  private val secondaryPluginResolverProvider: PluginResolverProvider? = null,
  ideModulePredicate: IdeModulePredicate = NegativeIdeModulePredicate,
  private val dependenciesModifier: DependenciesModifier = PassThruDependenciesModifier
) : PluginResolverProvider {

  private val dependencyTree = DependencyTree(pluginProvider, ideModulePredicate)

  private val dependencyResolverCache = Caffeine.newBuilder()
    .maximumSize(DEFAULT_CACHE_SIZE)
    .recordStats()
    .build<PluginArtifactKey, Resolver>()

  private val pluginResolverCache = Caffeine.newBuilder()
    .maximumSize(DEFAULT_CACHE_SIZE)
    .recordStats()
    .build<PluginResolverKey, NamedResolver>()

  /**
   * Provide a unified resolver for all transitive dependencies of this plugin.
   * The actual plugin classpath is *not* included in this resolver.
   *
   * This mechanism functions as a classpath filter.
   * It takes classes and resource bundles from the IDE
   * and includes only those that are declared as dependencies in the plugin.
   */
  override fun getResolver(plugin: IdePlugin): Resolver {
    plugin.id ?: return EMPTY_RESOLVER
    getFromSecondaryCache(plugin)?.let {
      return it
    }
    val cacheKey = plugin.artifactKey()
    // Invocation of `getIfPresent` is intentional!
    // Using `get` would lead to a recursive update triggered by `createResolver`.
    val resolver = dependencyResolverCache.getIfPresent(cacheKey)
    return resolver ?: createResolver(plugin).also {
      dependencyResolverCache.put(cacheKey, it)
    }
  }

  override fun contains(pluginId: PluginId): Boolean {
    pluginResolverCache.asMap().keys.firstOrNull { it.resolverName == pluginId }?.let { key ->
      return pluginResolverCache.getIfPresent(key) != null
    }

    dependencyResolverCache.asMap().keys.firstOrNull { it.id == pluginId }?.let { key ->
      return dependencyResolverCache.getIfPresent(key) != null
    }

    return false
  }

  /**
   * Returns the resolver containing the plugin's own classes if it is already available.
   * Unlike [getResolver], this method never returns a dependency-closure resolver.
   */
  fun getCachedPluginResolver(plugin: IdePlugin): Resolver? {
    getFromSecondaryCache(plugin)?.let {
      return it
    }
    return pluginResolverCache.getIfPresent(plugin.resolverCacheKey())
  }

  private fun createResolver(plugin: IdePlugin): Resolver {
    val dependencyTreeResolution = dependencyTree
      .getDependencyTreeResolution(plugin, dependenciesModifier = dependenciesModifier)

    val transitiveDependencies = dependencyTreeResolution
      .transitiveDependencies
      .filterNot { dep -> dep.pluginId == plugin.id }

    val resolvers = transitiveDependencies
      .mapNotNull { dep -> dep.pluginId?.let { it.intern() to dep } }
      .distinctBy { it.first }
      .associate { (id, dep) ->
        val dependencyPlugin = dep.plugin
        if (dependencyPlugin == null) {
          return@associate id to EmptyResolver(dep.id)
        }
        val resolverCacheKey = dependencyPlugin.resolverCacheKey()
        val dependencyResolver = pluginResolverCache.getIfPresent(resolverCacheKey)
        if (dependencyResolver != null) {
          return@associate id to dependencyResolver
        }
        // it's OK to synchronize on plugin id (String) since we've interned it.
        // Synchronizing to prevent creating different resolvers for the same plugin in `createResolverTree`.
        // Different plugin versions may serialize on the same lock, but they use distinct cache keys.
        synchronized(id) {
          val resolver = pluginResolverCache.getIfPresent(resolverCacheKey)
          if (resolver != null) {
            id to resolver
          } else {
            id to dep.createResolverTree()
          }
        }
      }
    return DependencyTreeAwareResolver.of(plugin.id ?: UNNAMED_RESOLVER, resolvers, dependencyTreeResolution)
  }

  private fun Dependency.createResolverTree(): NamedResolver {
    val dependencyPlugin = plugin ?: return EmptyResolver(id)
    return dependencyPlugin.createResolverTree()
      .let { (r, resolversToCache) ->
        pluginResolverCache.put(dependencyPlugin.resolverCacheKey(), r)
        resolversToCache.forEach {
          pluginResolverCache.put(dependencyPlugin.resolverCacheKey(it.name), it)
        }
        r
      }
  }

  private val Dependency.id: String
    get() {
      return when (this) {
        is Dependency.Module -> pluginId
        is Dependency.Plugin -> pluginId
        Dependency.None -> null
      } ?: UNKNOWN_DEPENDENCY_ID
    }

  fun getStats(): CacheStats? {
    return dependencyResolverCache.stats().plus(pluginResolverCache.stats())
  }

  private val IdePlugin.id: String?
    get() = pluginId ?: pluginName

  private fun IdePlugin.createResolverTree(): Pair<NamedResolver, List<NamedResolver>> {
    getFromSecondaryCache(this)?.let { pluginResolver ->
      val definedModuleResolvers = definedModules.map { moduleId ->
        getFromSecondaryCache(moduleId) ?: pluginResolver //FIXME document fallback pluginResolver when wrong product-info.json
      }.unique()

      val resultResolver = if (definedModuleResolvers.isNotEmpty()) {
        composeUniqueResolvers(newResolverName(), pluginResolver, definedModuleResolvers)
      } else {
        pluginResolver
      }

      return resultResolver to definedModuleResolvers
    }

    val resolverPrefix = pluginId?.let { "$it/" } ?: ""

    val resolversToCache = mutableListOf<NamedResolver>()
    val cpResolvers = classpath.entries.map { cpEntry ->
      val origin = IdeFileOrigin.BundledPlugin(cpEntry.path, idePlugin = this)
      val cpEntryResolverName = resolverPrefix + cpEntry.path.fileName.toString()
      val cpEntryResolver = pluginResolverCache.getIfPresent(resolverCacheKey(cpEntryResolverName))
      if (cpEntryResolver is NamedResolver) {
        return@map cpEntryResolver
      }
      (if (cpEntryResolver != null) {
        CompositeResolver.create(listOf(cpEntryResolver), cpEntryResolverName)
      } else {
        getFromSecondaryCache(cpEntryResolverName)
          ?: LazyJarResolver(cpEntry.path, readMode = ReadMode.SIGNATURES, origin, cpEntryResolverName)
      }).also {
        resolversToCache += it
      }
    }
    definedModules.forEach { moduleName ->
      resolversToCache += CompositeResolver.create(cpResolvers, moduleName)
    }

    return cpResolvers.asResolver(newResolverName()) to resolversToCache
  }

  private fun getFromSecondaryCache(id: PluginId): NamedResolver? {
    return getFromSecondaryCache(IdePluginImpl().apply { this.pluginId = id })
  }

  private fun getFromSecondaryCache(plugin: IdePlugin): NamedResolver? {
    plugin.id?.let { id ->
      if (secondaryPluginResolverProvider?.contains(id) == true) {
        val secondaryCachedResolver = secondaryPluginResolverProvider.getResolver(plugin)
        return secondaryCachedResolver.asNamed(id)
      }
    }
    return null
  }

  private fun Resolver.asNamed(fallbackName: String): NamedResolver {
    return this as? NamedResolver ?: CompositeResolver.create(listOf(this), fallbackName)
  }

  private fun IdePlugin.artifactKey(): PluginArtifactKey =
    PluginArtifactKey(id ?: UNNAMED_RESOLVER, pluginVersion, originalFile)

  private fun IdePlugin.resolverCacheKey(resolverName: String = newResolverName()): PluginResolverKey =
    PluginResolverKey(artifactKey(), resolverName)

  private fun IdePlugin.newResolverName(): String = id ?: UNNAMED_RESOLVER

  private val Dependency.plugin: IdePlugin?
    get() = when (this) {
      is Dependency.Module -> plugin
      is Dependency.Plugin -> plugin
      else -> null
    }

  private val Dependency.pluginId: String?
    get() = when (this) {
      is Dependency.Module -> plugin.id
      is Dependency.Plugin -> plugin.id
      else -> null
    }

  private fun composeUniqueResolvers(resolverName: String, resolver: NamedResolver, moreResolvers: Collection<NamedResolver>): NamedResolver {
    val resolvers = (listOf(resolver) + moreResolvers).unique()
    if (resolvers.size == 1) {
      return resolver
    }
    return CompositeResolver.create(resolvers, resolverName)
  }

  class DependencyTreeAwareResolver private constructor(
    name: String,
    private val resolverNames: Set<String>,
    internal val components: Collection<Resolver>,
    val dependencyTreeResolution: DependencyTreeResolution,
    delegateProvider: () -> Resolver
  ) : DelegatingNamedResolver(name, delegateProvider) {

    fun containsResolverName(resolverName: String): Boolean = resolverName in resolverNames

    override fun toString(): String {
      return "$name with ${resolverNames.size} resolvers: " + resolverNames.joinToString(",")
    }

    companion object {
      fun of(
        name: String, resolvers: Map<String, Resolver>, dependencyTreeResolution: DependencyTreeResolution
      ): DependencyTreeAwareResolver {
        return DependencyTreeAwareResolver(name, resolvers.keys, resolvers.values, dependencyTreeResolution) {
          LazyCompositeResolver.create(resolvers.values, name)
        }
      }
    }
  }
}

