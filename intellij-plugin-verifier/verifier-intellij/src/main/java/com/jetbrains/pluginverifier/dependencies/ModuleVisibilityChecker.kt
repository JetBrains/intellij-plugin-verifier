/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.dependencies

import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.ModuleDescriptor
import com.jetbrains.plugin.structure.intellij.plugin.ModuleVisibility
import com.jetbrains.plugin.structure.intellij.plugin.dependencies.PluginAware
import com.jetbrains.plugin.structure.intellij.plugin.module.IdeModule
import com.jetbrains.pluginverifier.PluginVerificationDescriptor
import com.jetbrains.pluginverifier.results.problems.ModuleVisibilityProblem
import com.jetbrains.pluginverifier.verifiers.PluginVerificationContext
import com.jetbrains.pluginverifier.verifiers.ProblemRegistrar

/**
 * A magic constant given to non-module plugins, so that we can uniformly handle namespace checks
 */
const val MODULE_PLACEHOLDER_STRING = "\$main_module"


/**
 * Checks visibility rules for content module dependencies.
 *
 * By default, a content module has `private` visibility:
 * - **PRIVATE** (default): only other modules from the same plugin can declare a dependency on it.
 * - **INTERNAL**: the module can be used as a dependency by other modules from the same plugin
 *   and modules from other plugins declared in the same namespace.
 * - **PUBLIC**: the module can be used as a dependency from any module of any plugin.
 *
 * Note: Main modules of plugins are referenced via the plugin ID (not module name),
 * so they are effectively public.
 */
class ModuleVisibilityChecker private constructor(private val ide: Ide, private val mainPlugin: IdePlugin) {
  data class ResolvedModuleInfoFrom(val parent: IdePlugin, val namespace: String)
  data class ResolvedModuleInfoTo(val parent: IdePlugin, val namespace: String, val visibility: ModuleVisibility)

  /**
   * Finds the bundled plugin that declares the content module descriptor [moduleName].
   *
   * [Ide.findPluginByModule] returns the first bundled plugin claiming [moduleName], which may be
   * the standalone [IdeModule] itself if it precedes its owner in the bundled plugins.
   * Standalone [IdeModule]s are never owners.
   */
  private fun findContentModuleOwner(moduleName: String): IdePlugin? {
    val plugin = ide.findPluginByModule(moduleName)
    if (plugin != null && plugin.declaresContentModule(moduleName)) {
      return plugin
    }
    return ide.bundledPlugins.firstOrNull { it.declaresContentModule(moduleName) }
  }

  private fun IdePlugin.declaresContentModule(moduleName: String): Boolean =
    this !is IdeModule && findContentModuleDescriptor(moduleName) != null

  /**
   * Checks if [dependingModule] can access [targetModule].
   *
   * @param dependingModule the module that declares the dependency (from plugin A)
   * @param targetModule the module being depended upon (from plugin B)
   * @return `true` if access is allowed, `false` if it violates visibility rules
   */
  fun isAccessAllowed(
    dependingModule: ResolvedModuleInfoFrom,
    targetModule: ResolvedModuleInfoTo
  ): Boolean {
    return when (targetModule.visibility) {
      ModuleVisibility.PUBLIC -> true
      ModuleVisibility.INTERNAL -> {
        // Access allowed if both modules are in the same plugin, or both are in the same namespace
        val samePlugin = dependingModule.parent === targetModule.parent
        val sameNamespace = dependingModule.namespace == targetModule.namespace
        samePlugin || sameNamespace
      }
      ModuleVisibility.PRIVATE -> {
        // Access allowed only by other modules in the same plugin
        dependingModule.parent === targetModule.parent
      }
    }
  }

  /**
   * Resolves module info for the source of a dependency edge (the module declaring the dependency).
   *
   * For [IdeModule] instances, finds the owning plugin, i.e. the bundled plugin that declares
   * the content module descriptor (regardless of the bundled-plugin order), and extracts the namespace.
   * For regular plugins, uses the namespace from its first module descriptor, or a placeholder
   * if this is the main plugin being verified.
   *
   * @return resolved module info, or `null` if the plugin cannot be resolved (e.g., legacy plugin)
   */
  fun resolveModuleInfoFrom(plugin: IdePlugin): ResolvedModuleInfoFrom? {
    if (plugin is IdeModule) {
      val parentPlugin = findContentModuleOwner(plugin.pluginId) ?: return null
      val moduleDescriptor = parentPlugin.findContentModuleDescriptor(plugin.pluginId) ?: return null

      return ResolvedModuleInfoFrom(parentPlugin, moduleDescriptor.moduleDefinition.actualNamespace)
    } else {
      return resolveModuleInfoFromProperPlugin(plugin)
    }
  }

  /**
   * Resolve module info from a proper plugin.
   * We assume that all modules in a plugin share the same namespace, including the main module.
   * If there are no modules at all, we give the magic string `MODULE_PLACEHOLDER_STRING`,
   * so that relevant visibility checks can still be computed.
   * @see [com.jetbrains.plugin.structure.intellij.plugin.PluginModuleResolver.resolvePluginModules]
  */
  private fun resolveModuleInfoFromProperPlugin(plugin: IdePlugin): ResolvedModuleInfoFrom? {
    val namespace = plugin.modulesDescriptors
      .firstOrNull()
      ?.moduleDefinition
      ?.actualNamespace
      ?: MODULE_PLACEHOLDER_STRING.takeIf { plugin == mainPlugin }
      ?: return null

    return ResolvedModuleInfoFrom(plugin, namespace)
  }

  /** Resolves the declaring module by its graph identity, retaining its owning plugin. */
  fun resolveModuleInfoFrom(node: DependencyNode): ResolvedModuleInfoFrom? = when (node) {
    is DependencyNode.PluginDependency -> resolveModuleInfoFrom(node.plugin)
    is DependencyNode.ModuleDependency -> resolveModuleInfoFrom(node.plugin, node.moduleName)
    is DependencyNode.ContentModuleDeclaration -> resolveModuleInfoFrom(node.owner, node.name)
    is DependencyNode.IdAndVersionDependency -> null
  }

  private fun resolveModuleInfoFrom(plugin: IdePlugin, contentModuleName: String): ResolvedModuleInfoFrom? {
    val descriptor = plugin.findContentModuleDescriptor(contentModuleName) ?: return null
    return ResolvedModuleInfoFrom(plugin, descriptor.moduleDefinition.actualNamespace)
  }

  /**
   * Finds a content module descriptor within the current plugin based on the given content module name.
   *
   * @param contentModuleName the name of the module to find. If `null`, the method will return `null`.
   * @return the matching module descriptor if found, or `null` otherwise.
   */
  private fun IdePlugin.findContentModuleDescriptor(contentModuleName: String?): ModuleDescriptor? =
    modulesDescriptors.firstOrNull { it.name == contentModuleName }

  /**
   * Resolves module info for the target of a dependency edge (the module being depended upon).
   *
   * For [IdeModule] instances, finds the owning plugin, i.e. the bundled plugin that declares
   * the content module descriptor (regardless of the bundled-plugin order), and extracts namespace and visibility.
   * For regular plugins referenced via `<plugin>` in dependencies, looks up the main module.
   *
   * @return resolved module info including visibility, or `null` if the plugin cannot be resolved
   */
  fun resolveModuleInfoTo(plugin: IdePlugin): ResolvedModuleInfoTo? {
    if (plugin is IdeModule) {
      val parentPlugin = findContentModuleOwner(plugin.pluginId) ?: return null
      val moduleDescriptor = parentPlugin.findContentModuleDescriptor(plugin.pluginId) ?: return null

      return ResolvedModuleInfoTo(parentPlugin, moduleDescriptor.moduleDefinition.actualNamespace, moduleDescriptor.module.moduleVisibility)
    } else {
      // In case of a <plugin> within <dependencies>, we consider the main module if it exists
      val moduleDescriptor = plugin.findContentModuleDescriptor(plugin.pluginId) ?: return null

      return ResolvedModuleInfoTo(plugin, moduleDescriptor.moduleDefinition.actualNamespace, moduleDescriptor.module.moduleVisibility)
    }
  }

  /** Ownership declarations are not dependency targets and have no visibility to check. */
  fun resolveModuleInfoTo(node: DependencyNode): ResolvedModuleInfoTo? = when (node) {
    is DependencyNode.PluginDependency -> resolveModuleInfoTo(node.plugin)
    is DependencyNode.ModuleDependency -> {
      val descriptor = node.plugin.findContentModuleDescriptor(node.moduleName)
      descriptor?.let {
        ResolvedModuleInfoTo(node.plugin, it.moduleDefinition.actualNamespace, it.module.moduleVisibility)
      }
    }
    is DependencyNode.ContentModuleDeclaration, is DependencyNode.IdAndVersionDependency -> null
  }

  /**
   * Iterates over [dependenciesGraph] edges and registers a [ModuleVisibilityProblem] for every
   * dependency declared by the [verified plugin][DependenciesGraph.verifiedPlugin], including
   * its content modules, that violates module-visibility rules.
   *
   * Ownership edges and declarations owned by other plugins (i.e. transitive dependency edges
   * such as B → C in a chain A → B → C) are intentionally skipped.
   */
  fun checkEdges(dependenciesGraph: DependenciesGraph, problemRegistrar: ProblemRegistrar) {
    val verifiedPlugin = (dependenciesGraph.verifiedPlugin as? PluginAware)?.plugin ?: return
    for ((a, b) in dependenciesGraph.edges) {
      if (a !is PluginAware || b !is PluginAware || a.plugin != verifiedPlugin || b is DependencyNode.ContentModuleDeclaration) {
        continue
      }
      // The dependency graph can contain legacy plugins as well as content modules.
      // Visibility checks only apply between content modules, so failure on resolution will simply skip the edge with no warnings
      val from = resolveModuleInfoFrom(a) ?: continue
      val to = resolveModuleInfoTo(b) ?: continue

      if (!isAccessAllowed(from, to)) {
        problemRegistrar.registerProblem(ModuleVisibilityProblem.create(a.id, from, b.id, to))
      }
    }
  }

  companion object {
    const val VISIBILITY_CHECK_INTRODUCED_VERSION = 261

    /**
     * Returns `true` if [context] targets an IDE version that supports module visibility checks.
     * Must be called before [build].
     */
    fun supports(context: PluginVerificationContext): Boolean =
      context.verificationDescriptor is PluginVerificationDescriptor.IDE
        && context.verificationDescriptor.ideVersion.components[0] >= VISIBILITY_CHECK_INTRODUCED_VERSION

    /**
     * Builds a [ModuleVisibilityChecker] for the given [context].
     * Requires [supports] to return `true`; throws [IllegalStateException] otherwise.
     */
    fun build(context: PluginVerificationContext): ModuleVisibilityChecker {
      check(supports(context)) {
        "ModuleVisibilityChecker is not applicable for this context. Call isApplicable() before build()."
      }
      val descriptor = context.verificationDescriptor as PluginVerificationDescriptor.IDE
      return ModuleVisibilityChecker(descriptor.ide, context.idePlugin)
    }
  }
}