/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide.classes.resolver

import com.jetbrains.plugin.structure.classes.resolvers.CompositeResolver
import com.jetbrains.plugin.structure.classes.resolvers.EmptyResolver
import com.jetbrains.plugin.structure.classes.resolvers.JarFileResolver
import com.jetbrains.plugin.structure.classes.resolvers.ResolutionResult
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.ide.classes.IdeFileOrigin
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.dependencies.DefaultDependenciesProvider
import com.jetbrains.plugin.structure.intellij.plugin.dependencies.DependenciesProvider
import com.jetbrains.plugin.structure.intellij.plugin.dependencies.Dependency
import org.objectweb.asm.tree.ClassNode
import java.util.*

private const val UNNAMED_RESOLVER = "Unnamed Resolver"

private val EMPTY_UNNAMED_RESOLVER = NamedResolver(UNNAMED_RESOLVER, EmptyResolver)

/**
 * A classpath resolver that matches plugin dependencies against bundled plugins of an `product-info.json`-based IDE.
 *
 * This resolver functions as a classpath filter.
 * It takes classes and resource bundles from the IDE
 * and includes only those that are declared as dependencies in the plugin.
 */
class PluginDependencyFilteredResolver(
  plugin: IdePlugin,
  productInfoClassResolver: ProductInfoClassResolver,
  private val dependenciesProvider: DependenciesProvider = DefaultDependenciesProvider(productInfoClassResolver.ide)
) : Resolver() {
  private val filteredResolvers: List<NamedResolver> = getResolvers(plugin)

  private fun getResolvers(plugin: IdePlugin): List<NamedResolver> {
    return dependenciesProvider.getDependencies(plugin).map {
      it.plugin?.asResolver() ?: EMPTY_UNNAMED_RESOLVER
    }
  }

  private val delegateResolver = filteredResolvers.asResolver()

  override val readMode get() = delegateResolver.readMode

  override val allClasses get() = delegateResolver.allClasses

  override val allPackages get() = delegateResolver.allPackages

  override val allBundleNameSet get() = delegateResolver.allBundleNameSet

  override fun resolveClass(className: String) = delegateResolver.resolveClass(className)

  override fun resolveExactPropertyResourceBundle(baseName: String, locale: Locale) =
    delegateResolver.resolveExactPropertyResourceBundle(baseName, locale)

  override fun containsClass(className: String) = delegateResolver.containsClass(className)

  override fun containsPackage(packageName: String) = delegateResolver.containsPackage(packageName)

  override fun processAllClasses(processor: (ResolutionResult<ClassNode>) -> Boolean) =
    delegateResolver.processAllClasses(processor)

  override fun close() = delegateResolver.close()

  fun containsResolverName(resolverName: String): Boolean = filteredResolvers.any { it.name == resolverName }

  val size: Int = filteredResolvers.size

  private fun List<Resolver>.asResolver(): Resolver {
    return CompositeResolver.create(this)
  }

  private fun List<Resolver>.asNamedResolver(resolverName: String): NamedResolver {
    return NamedResolver(resolverName, CompositeResolver.create(this, resolverName))
  }

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

  private val Dependency.plugin: IdePlugin?
    get() = when (this) {
      is Dependency.Module -> plugin
      is Dependency.Plugin -> plugin
      else -> null
    }

  private fun IdePlugin.newResolverName(): String = id ?: UNNAMED_RESOLVER
}

