package com.jetbrains.plugin.structure.intellij.plugin.dependencies

import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.plugin.structure.intellij.plugin.PluginProvider
import com.jetbrains.plugin.structure.intellij.plugin.dependencies.Dependency.*
import com.jetbrains.plugin.structure.intellij.plugin.module.IdeModule
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val LOG: Logger = LoggerFactory.getLogger(DependencyTree::class.java)

class DependencyTree(private val pluginProvider: PluginProvider) {
  @Throws(IllegalArgumentException::class)
  fun getTransitiveDependencies(plugin: IdePlugin): Set<Dependency> {
    val pluginId: PluginId = requireNotNull(plugin.pluginId) { "Plugin must have an ID" }
    val graph = getDependencyGraph(plugin)
    return graph.collectDependencies(pluginId)
  }

  private fun getDependencyGraph(plugin: IdePlugin): DiGraph<PluginId, Dependency> {
    val graph = DiGraph<PluginId, Dependency>()
    getDependencyGraph(plugin, graph, 0)
    return graph
  }

  private fun getDependencyGraph(plugin: IdePlugin, graph: DiGraph<PluginId, Dependency>, resolutionDepth: Int): Unit =
    with(plugin) {
      val pluginId = pluginId ?: return@with
      val indent = "  ".repeat(resolutionDepth)
      LOG.atDebug().log("${indent}Resolving ${dependencies.size} dependencies for '$pluginId': " + dependencies.joinToString { it.id })
      dependencies.forEachIndexed { i, dep ->
        val dependencyPlugin = pluginProvider.getPluginOrModule(dep.id)
        if (ignore(plugin, dep)) {
          LOG.atDebug().log("$indent  ${i + 1}) Plugin declares dependency '${dep.id}' in its modules")
          return@forEachIndexed
        }
        if (graph.contains(pluginId, dependencyPlugin)) {
          LOG.atDebug().log("$indent  ${i + 1}) Resolved cached dependency ${dep.id}")
          return@forEachIndexed
        }
        LOG.atDebug().log("$indent  ${i + 1}) Resolving dependency for ${dep.id}")
        when (dependencyPlugin) {
          is Module,
          is Plugin -> {
            @Suppress("USELESS_IS_CHECK")
            if (dependencyPlugin is PluginAware && !dependencyPlugin.matches(pluginId)) {
              graph.addEdge(pluginId, dependencyPlugin)
              getDependencyGraph(dependencyPlugin.plugin, graph, resolutionDepth + 1)
            }
          }

          is None -> LOG.atDebug().log("$indent  Plugin '$pluginId' depends on a plugin '${dep.id}' that is not resolved in the IDE. Skipping")
        }
      }
    }

  private fun ignore(plugin: IdePlugin, dependency: PluginDependency): Boolean {
    return dependency.isModule && plugin.definedModules.contains(dependency.id)
  }

  private fun DiGraph<PluginId, Dependency>.collectDependencies(id: PluginId): Set<Dependency> {
    return mutableSetOf<Dependency>().apply {
      collectDependencies(id, this)
    }
  }

  private fun DiGraph<PluginId, Dependency>.collectDependencies(id: PluginId, dependencies: MutableSet<Dependency>, layer: Int = 0) {
    for (dependency in this[id]) {
      if (dependency is PluginAware) {
        val depId = dependency.plugin.pluginId!!
        val dep = if (layer == 0) dependency else dependency.asTransitive()
        if (dep !in dependencies) {
          dependencies += dep
          collectDependencies(depId, dependencies, layer + 1)
        }
      }
    }
  }

  private fun PluginProvider.getPluginOrModule(id: String): Dependency {
    val plugin = this.getPluginById(id)
    return if (plugin != null) {
      if (plugin is IdeModule) {
        Module(plugin, id)
      } else {
        Plugin(plugin)
      }
    } else {
      this.getPluginByModule(id)?.let {
        Module(it, id)
      } ?: None
    }
  }

  private fun DiGraph<PluginId, Dependency>.toDebugString(id: PluginId, indentSize: Int, visited: MutableSet<PluginId>, printer: StringBuilder) {
    val indent = "  ".repeat(indentSize)
    this[id].forEach { dep ->
      if (dep is PluginAware) {
        val depId = dep.plugin.pluginId!!
        if (depId !in visited) {
          visited += depId
          printer.appendLine("${indent}* " + dep)
          toDebugString(depId, indentSize + 1, visited, printer)
        } else {
          printer.appendLine("${indent}* $dep (already visited)")
        }
      }
    }
  }

  private fun Dependency.asTransitive(): Dependency {
    return when (this) {
      is Module -> copy(isTransitive = true)
      is Plugin -> copy(isTransitive = true)
      is None -> this
    }
  }

  fun toDebugString(pluginId: String): CharSequence {
    return StringBuilder().apply {
      DiGraph<PluginId, Dependency>().toDebugString(pluginId, 0, mutableSetOf(), printer = this)
    }
  }
}