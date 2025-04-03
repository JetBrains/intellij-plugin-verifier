/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide.classes.resolver

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.stats.CacheStats
import com.jetbrains.plugin.structure.classes.resolvers.EmptyResolver
import com.jetbrains.plugin.structure.classes.resolvers.LazyCompositeResolver
import com.jetbrains.plugin.structure.classes.resolvers.LazyJarResolver
import com.jetbrains.plugin.structure.classes.resolvers.ResolutionResult
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.classes.resolvers.Resolver.ReadMode
import com.jetbrains.plugin.structure.classes.resolvers.ResourceBundleNameSet
import com.jetbrains.plugin.structure.ide.classes.IdeFileOrigin
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.PluginProvider
import com.jetbrains.plugin.structure.intellij.plugin.dependencies.Dependency
import com.jetbrains.plugin.structure.intellij.plugin.dependencies.DependencyTree
import com.jetbrains.plugin.structure.intellij.plugin.dependencies.PluginId
import org.objectweb.asm.tree.ClassNode
import java.util.*

private const val UNNAMED_RESOLVER = "Unnamed Resolver"
private val EMPTY_UNNAMED_RESOLVER = NamedResolver(UNNAMED_RESOLVER, EmptyResolver)

/**
 * See also cache size in [com.jetbrains.plugin.structure.classes.resolvers.CacheResolver].
 */
private const val DEFAULT_CACHE_SIZE = 1024L

class CachingPluginDependencyResolverProvider(pluginProvider: PluginProvider) : PluginResolverProvider {

  private val dependencyTree = DependencyTree(pluginProvider)

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
    val id = plugin.id ?: return EMPTY_UNNAMED_RESOLVER
    // Invocation of `getIfPresent` is intentional!
    // Using `get` would lead to a recursive update triggered by `doGetResolver`.
    val resolver = cache.getIfPresent(id)
    return resolver ?: createResolver(plugin).also {
      cache.put(id, it)
    }
  }

  override fun contains(pluginId: PluginId) = cache.getIfPresent(pluginId) != null

  private fun createResolver(plugin: IdePlugin): Resolver {
    val transitiveDependencies = dependencyTree
      .getTransitiveDependencies(plugin)
      .filterNot { dep -> dep.pluginId == plugin.id }
    val resolvers = transitiveDependencies.mapNotNull { dep ->
      dep.pluginId?.let { id ->
        id to cache.get(id) {
          dep.resolver
        }
      }
    }
    return ComponentNameAwareCompositeResolver(plugin.id ?: UNNAMED_RESOLVER, resolvers)
  }

  fun getStats(): CacheStats? {
    return cache.stats()
  }

  private val IdePlugin.id: String?
    get() = pluginId ?: pluginName

  private fun IdePlugin?.asResolver(): Resolver {
    if (this == null) return EMPTY_UNNAMED_RESOLVER

    return classpath.entries.map {
      val origin = IdeFileOrigin.BundledPlugin(it.path, idePlugin = this)
      LazyJarResolver(it.path, readMode = ReadMode.FULL, origin)
    }.asResolver(newResolverName())
  }

  private fun List<Resolver>.asResolver(resolverName: String): Resolver {
    return LazyCompositeResolver.create(this, resolverName)
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

  private val Dependency.resolver: Resolver
    get() = plugin?.asResolver() ?: EMPTY_UNNAMED_RESOLVER

  class ComponentNameAwareCompositeResolver(
    name: String,
    resolvers: List<Pair<String, Resolver>>
  ) : Resolver() {
    private val resolverNames = resolvers.map { it.first }.toSet()

    private val delegateResolver = LazyCompositeResolver.create(resolvers.map { it.second }, name)

    override val readMode: ReadMode
      get() = delegateResolver.readMode
    override val allClasses: Set<String>
      get() = delegateResolver.allClasses
    override val allPackages: Set<String>
      get() = delegateResolver.allPackages
    override val allBundleNameSet: ResourceBundleNameSet
      get() = delegateResolver.allBundleNameSet

    override fun resolveClass(className: String) = delegateResolver.resolveClass(className)

    override fun resolveExactPropertyResourceBundle(
      baseName: String,
      locale: Locale
    ) = delegateResolver.resolveExactPropertyResourceBundle(baseName, locale)

    override fun containsClass(className: String) = delegateResolver.containsClass(className)

    override fun containsPackage(packageName: String) = delegateResolver.containsPackage(packageName)

    override fun processAllClasses(processor: (ResolutionResult<ClassNode>) -> Boolean) =
      delegateResolver.processAllClasses(processor)

    override fun close() = delegateResolver.close()

    fun containsResolverName(resolverName: String): Boolean = resolverName in resolverNames
  }
}

