/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide.classes.resolver

import com.github.benmanes.caffeine.cache.Caffeine
import com.jetbrains.plugin.structure.classes.resolvers.CompositeResolver
import com.jetbrains.plugin.structure.classes.resolvers.EmptyResolver
import com.jetbrains.plugin.structure.classes.resolvers.JarFileResolver
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
    .maximumSize(512).build<PluginId, NamedResolver>()

  private val nameCache = Caffeine.newBuilder()
    .maximumSize(512).build<String, Boolean>()

  override fun getResolver(plugin: IdePlugin): Resolver {
    return cache.get(plugin.id) {
      dependencyTree.getTransitiveDependencies(plugin).map { dependency ->
        dependency.resolver.also {
          cache(dependency)
        }
      }.asNamedResolver(plugin.id ?: UNNAMED_RESOLVER)
    }
  }

  override fun contains(pluginId: PluginId) = nameCache.asMap().containsKey(pluginId)

  private val IdePlugin.id: String?
    get() = pluginId ?: pluginName

  private fun IdePlugin?.asResolver(): NamedResolver {
    if (this == null) return EMPTY_UNNAMED_RESOLVER

    return classpath.entries.map {
      val origin = IdeFileOrigin.BundledPlugin(it.path, idePlugin = this)
      //FIXME check readmode
      JarFileResolver(it.path, readMode = ReadMode.FULL, origin)
    }.asNamedResolver(newResolverName())
  }

  private fun List<Resolver>.asNamedResolver(resolverName: String): NamedResolver {
    return NamedResolver(resolverName, CompositeResolver.create(this, resolverName))
  }

  private fun IdePlugin.newResolverName(): String = id ?: UNNAMED_RESOLVER

  private val Dependency.plugin: IdePlugin?
    get() = when (this) {
      is Dependency.Module -> plugin
      is Dependency.Plugin -> plugin
      else -> null
    }

  private val Dependency.resolver: NamedResolver
    get() = plugin?.asResolver() ?: EMPTY_UNNAMED_RESOLVER

  private fun cache(dependency: Dependency) {
    dependency.plugin?.id?.let { pluginId -> nameCache.put(pluginId, true) }
  }
}

