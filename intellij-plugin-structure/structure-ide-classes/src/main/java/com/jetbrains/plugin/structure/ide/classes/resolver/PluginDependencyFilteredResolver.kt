package com.jetbrains.plugin.structure.ide.classes.resolver

import com.jetbrains.plugin.structure.classes.resolvers.CompositeResolver
import com.jetbrains.plugin.structure.classes.resolvers.ResolutionResult
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import org.objectweb.asm.tree.ClassNode
import java.util.*

/**
 * A classpath resolver that matches plugin dependencies against bundled plugins of an `product-info.json`-based IDE.
 *
 * This resolver functions as a classpath filter.
 * It takes classes and resource bundles from the IDE
 * and includes only those that are declared as dependencies in the plugin.
 */
class PluginDependencyFilteredResolver(plugin: IdePlugin, productInfoClassResolver: ProductInfoClassResolver) : Resolver() {
  val filteredResolvers: List<NamedResolver> = productInfoClassResolver.layoutComponentResolvers
    .filter { resolver ->
      plugin.dependencies.any { pluginDep -> pluginDep.id == resolver.name }
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

  private fun List<Resolver>.asResolver(): Resolver {
    return CompositeResolver.create(this)
  }
}