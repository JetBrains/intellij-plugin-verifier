/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide.classes.resolver

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.stats.CacheStats
import com.jetbrains.plugin.structure.base.BinaryClassName
import com.jetbrains.plugin.structure.classes.resolvers.CompositeResolver
import com.jetbrains.plugin.structure.classes.resolvers.EMPTY_RESOLVER
import com.jetbrains.plugin.structure.classes.resolvers.EmptyResolver
import com.jetbrains.plugin.structure.classes.resolvers.LazyCompositeResolver
import com.jetbrains.plugin.structure.classes.resolvers.LazyJarResolver
import com.jetbrains.plugin.structure.classes.resolvers.NamedResolver
import com.jetbrains.plugin.structure.classes.resolvers.ResolutionResult
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.classes.resolvers.Resolver.ReadMode
import com.jetbrains.plugin.structure.classes.resolvers.ResourceBundleNameSet
import com.jetbrains.plugin.structure.classes.resolvers.UNNAMED_RESOLVER
import com.jetbrains.plugin.structure.classes.resolvers.asResolver
import com.jetbrains.plugin.structure.ide.classes.IdeFileOrigin
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginImpl
import com.jetbrains.plugin.structure.intellij.plugin.PluginProvider
import com.jetbrains.plugin.structure.intellij.plugin.dependencies.Dependency
import com.jetbrains.plugin.structure.intellij.plugin.dependencies.DependencyTree
import com.jetbrains.plugin.structure.intellij.plugin.dependencies.DependencyTreeResolution
import com.jetbrains.plugin.structure.intellij.plugin.dependencies.PluginId
import com.jetbrains.plugin.structure.intellij.plugin.dependencies.legacy.LegacyPluginDependencyContributor
import org.objectweb.asm.tree.ClassNode
import java.util.*

/**
 * See also cache size in [com.jetbrains.plugin.structure.classes.resolvers.CacheResolver].
 */
private const val DEFAULT_CACHE_SIZE = 1024L

private const val UNKNOWN_DEPENDENCY_ID = "Unknown ID"

class CachingPluginDependencyResolverProvider(pluginProvider: PluginProvider, private val secondaryPluginResolverProvider: PluginResolverProvider? = null) : PluginResolverProvider {

  private val dependencyTree = DependencyTree(pluginProvider)

  private val dependenciesModifier = LegacyPluginDependencyContributor()

  private val cache = Caffeine.newBuilder()
    .maximumSize(DEFAULT_CACHE_SIZE)
    .recordStats()
    .build<PluginId, Resolver>()

  /**
   * Provide a unified resolver for all transitive dependencies of this plugin.
   * The actual plugin classpath is *not* included in this resolver.
   *
   * This mechanism functions as a classpath filter.
   * It takes classes and resource bundles from the IDE
   * and includes only those that are declared as dependencies in the plugin.
   */
  override fun getResolver(plugin: IdePlugin): Resolver {
    val id = plugin.id ?: return EMPTY_RESOLVER
    getFromSecondaryCache(plugin)?.let {
      return it.also {
        cache.put(id, it)
      }
    }
    // Invocation of `getIfPresent` is intentional!
    // Using `get` would lead to a recursive update triggered by `doGetResolver`.
    val resolver = cache.getIfPresent(id)
    return resolver ?: createResolver(plugin).also {
      cache.put(id, it)
    }
  }

  override fun contains(pluginId: PluginId) = cache.getIfPresent(pluginId) != null

  private fun createResolver(plugin: IdePlugin): Resolver {
    val dependencyTreeResolution = dependencyTree
      .getDependencyTreeResolution(plugin, dependenciesModifier = dependenciesModifier)

    val transitiveDependencies = dependencyTreeResolution
      .transitiveDependencies
      .filterNot { dep -> dep.pluginId == plugin.id }

    val resolvers = transitiveDependencies
      .mapNotNull { dep -> dep.pluginId?.let { it to dep } }
      .distinctBy { it.first }
      .associate { (id, dep) ->
        val dependencyResolver = cache.getIfPresent(id)
        if (dependencyResolver != null) {
          id to dependencyResolver
        } else {
          id to dep.createResolverTree()
        }
      }
    return DependencyTreeAwareResolver(plugin.id ?: UNNAMED_RESOLVER, resolvers, dependencyTreeResolution)
  }

  private fun Dependency.createResolverTree(): NamedResolver {
    return plugin?.createResolverTree()
      ?.let { (r, resolversToCache) ->
        cache.put(this.pluginId, r)
        resolversToCache.forEach {
          cache.put(it.name, it)
        }
        r
      } ?: EmptyResolver(id)
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
    return cache.stats()
  }

  private val IdePlugin.id: String?
    get() = pluginId ?: pluginName

  private fun IdePlugin.createResolverTree(): Pair<NamedResolver, List<NamedResolver>> {
    getFromSecondaryCache(this)?.let { pluginResolver ->
      val definedModuleResolvers = definedModules.map { moduleId ->
        getFromSecondaryCache(moduleId) ?: pluginResolver //FIXME document fallback pluginResolver when wrong product-info.json
      }

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
      val cpEntryResolver = cache.getIfPresent(cpEntryResolverName)
      if (cpEntryResolver != null) {
        cpEntryResolver as? NamedResolver ?: CompositeResolver.create(listOf(cpEntryResolver), cpEntryResolverName)
      } else {
        getFromSecondaryCache(cpEntryResolverName)
          ?: LazyJarResolver(cpEntry.path, readMode = ReadMode.SIGNATURES, origin, cpEntryResolverName).also {
            resolversToCache += it
          }
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
    val result = IdentityHashMap<NamedResolver, Unit>()
    result[resolver] = Unit
    moreResolvers.forEach { result[it] = Unit }
    return CompositeResolver.create(result.keys, resolverName)
  }

  class DependencyTreeAwareResolver(
    private val name: String,
    resolvers: Map<String, Resolver>,
    val dependencyTreeResolution: DependencyTreeResolution
  ) : Resolver() {
    private val resolverNames = resolvers.keys

    private val delegateResolver = LazyCompositeResolver.create(resolvers.values, name)

    internal val components = resolvers.values

    override val readMode: ReadMode
      get() = delegateResolver.readMode
    @Deprecated("Use 'allClassNames' property instead which is more efficient")
    override val allClasses: Set<String>
      get() = delegateResolver.allClasses
    override val allClassNames: Set<BinaryClassName>
      get() = delegateResolver.allClassNames
    @Deprecated("Use 'packages' property instead. This property may be slow on some file systems.")
    override val allPackages: Set<String>
      get() = delegateResolver.allPackages
    override val packages: Set<String>
      get() = delegateResolver.packages
    override val allBundleNameSet: ResourceBundleNameSet
      get() = delegateResolver.allBundleNameSet

    @Deprecated("Use 'resolveClass(BinaryClassName)' instead")
    override fun resolveClass(className: String) = delegateResolver.resolveClass(className)

    override fun resolveClass(className: BinaryClassName) = delegateResolver.resolveClass(className)

    override fun resolveExactPropertyResourceBundle(
      baseName: String,
      locale: Locale
    ) = delegateResolver.resolveExactPropertyResourceBundle(baseName, locale)

    @Deprecated("Use 'containsClass(BinaryClassName)' instead")
    override fun containsClass(className: String) = delegateResolver.containsClass(className)

    override fun containsClass(className: BinaryClassName) = delegateResolver.containsClass(className)

    override fun containsPackage(packageName: String) = delegateResolver.containsPackage(packageName)

    override fun processAllClasses(processor: (ResolutionResult<ClassNode>) -> Boolean) =
      delegateResolver.processAllClasses(processor)

    override fun close() = delegateResolver.close()

    fun containsResolverName(resolverName: String): Boolean = resolverName in resolverNames

    override fun toString(): String {
      return "$name with ${resolverNames.size} resolvers: " + resolverNames.joinToString(",")
    }
  }
}

