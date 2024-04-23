package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.intellij.problems.PluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.resources.ResourceResolver
import java.nio.file.Path
import java.util.*

internal data class PluginMetadataResolutionContext(
  val pluginFile: Path,
  val descriptorPath: String,
  val validateDescriptor: Boolean,
  val resourceResolver: ResourceResolver,
  val problemResolver: PluginCreationResultResolver,
  val parentPlugin: PluginCreator?,
)

internal fun interface PluginLoader {
  fun load(context: PluginMetadataResolutionContext): PluginCreator
}

internal class OptionalDependencyResolver(private val pluginLoader: PluginLoader) {

  fun resolveOptionalDependencies(
    plugin: PluginCreator,
    pluginFile: Path,
    resourceResolver: ResourceResolver,
    problemResolver: PluginCreationResultResolver
  ) {
    val dependencyChain = DependencyChain()
    resolveOptionalDependencies(plugin, mutableSetOf(), pluginFile, resourceResolver, problemResolver, dependencyChain)
    dependencyChain.cycles.forEach {
      plugin.registerOptionalDependenciesConfigurationFilesCycleProblem(it)
    }
  }

  /**
   * [plugin] - plugin whose optional dependencies are resolved (plugin.xml, then someOptional.xml, ...)
   */
  private fun resolveOptionalDependencies(
    plugin: PluginCreator,
    visitedConfigurationFiles: MutableSet<String>,
    pluginFile: Path,
    resourceResolver: ResourceResolver,
    problemResolver: PluginCreationResultResolver,
    dependencyChain: DependencyChain
  ) {
    if (!visitedConfigurationFiles.add(plugin.descriptorPath)) {
      return
    }
    dependencyChain.push(plugin)
    for ((pluginDependency, configurationFile) in plugin.optionalDependenciesConfigFiles) {
      if (dependencyChain.detectCycle(configurationFile)) {
        return
      }

      val optionalDependencyCreator = pluginLoader.load(PluginMetadataResolutionContext(pluginFile, configurationFile, false, resourceResolver, problemResolver, plugin))
      plugin.addOptionalDescriptor(pluginDependency, configurationFile, optionalDependencyCreator)
      resolveOptionalDependencies(optionalDependencyCreator, visitedConfigurationFiles, pluginFile, resourceResolver, problemResolver, dependencyChain)
    }
    dependencyChain.pop()
  }

  internal class DependencyChain {
    private val chain = LinkedList<String>()

    private val _dependencyCycles: MutableList<DependencyCycle> = mutableListOf()

    val cycles: List<DependencyCycle>
      get() = _dependencyCycles

    fun detectCycle(configurationFile: String): Boolean {
      if (chain.contains(configurationFile)) {
        _dependencyCycles.add(chain + configurationFile)
        return true
      }
      return false
    }

    fun push(plugin: PluginCreator) {
      chain.addLast(plugin.descriptorPath)
    }

    fun pop() {
      chain.removeLast()
    }
  }
}




typealias DependencyCycle = List<String>