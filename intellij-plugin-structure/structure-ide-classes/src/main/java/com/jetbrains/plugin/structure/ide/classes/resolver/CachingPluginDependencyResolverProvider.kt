/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide.classes.resolver

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.stats.CacheStats
import com.jetbrains.plugin.structure.classes.resolvers.EmptyResolver
import com.jetbrains.plugin.structure.classes.resolvers.LazyCompositeResolver
import com.jetbrains.plugin.structure.classes.resolvers.LazyJarResolver
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.classes.resolvers.Resolver.ReadMode
import com.jetbrains.plugin.structure.ide.classes.IdeFileOrigin
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.PluginProvider
import com.jetbrains.plugin.structure.intellij.plugin.dependencies.Dependency
import com.jetbrains.plugin.structure.intellij.plugin.dependencies.DependencyTree
import com.jetbrains.plugin.structure.intellij.plugin.dependencies.PluginId

private const val UNNAMED_RESOLVER = "Unnamed Resolver"

private val EMPTY_UNNAMED_RESOLVER = NamedResolver(UNNAMED_RESOLVER, EmptyResolver)

class CachingPluginDependencyResolverProvider(pluginProvider: PluginProvider) : PluginResolverProvider {

  private val dependencyTree = DependencyTree(pluginProvider)

  private val cache = Caffeine.newBuilder()
    .maximumSize(512)
    .recordStats()
    .build<PluginId, Resolver>()

  override fun getResolver(plugin: IdePlugin): Resolver {
    val id = plugin.id ?: return EMPTY_UNNAMED_RESOLVER
    // Invocation of `getIfPresent` is intentional!
    // Using `get` would lead to a recursive update triggered by `doGetResolver`.
    val resolver = cache.getIfPresent(id)
    return resolver ?: doGetResolver(plugin).also {
      cache.put(id, it)
    }
  }

  override fun contains(pluginId: PluginId) = cache.getIfPresent(pluginId) != null

  private fun doGetResolver(plugin: IdePlugin): Resolver {
    val transitiveDependencies = dependencyTree
      .getTransitiveDependencies(plugin)
      .filterNot { dep -> dep.pluginId == plugin.id }
    val resolvers = transitiveDependencies.mapNotNull { dep ->
      dep.pluginId?.let { id ->
        cache.get(id) {
          dep.resolver
        }
      }
    }
    // TODO is plugin classpath included in the resolver?
    return resolvers.asResolver(plugin.id ?: UNNAMED_RESOLVER)
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
      //FIXME check readmode
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

}

