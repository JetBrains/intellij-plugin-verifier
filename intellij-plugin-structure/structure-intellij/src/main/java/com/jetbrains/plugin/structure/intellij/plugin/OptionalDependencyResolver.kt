package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.intellij.problems.OptionalDependencyDescriptorResolutionProblem
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

typealias DependencyCycle = List<String>

internal class OptionalDependencyResolver(private val pluginLoader: PluginLoader) {

  fun resolveOptionalDependencies(
    plugin: PluginCreator,
    pluginFile: Path,
    resourceResolver: ResourceResolver,
    problemResolver: PluginCreationResultResolver
  ) {
    val dependencyChain = DependencyChain()
    resolveOptionalDependencies(plugin, pluginFile, resourceResolver, problemResolver, dependencyChain)
    dependencyChain.cycles.forEach {
      plugin.registerOptionalDependenciesConfigurationFilesCycleProblem(it)
    }
  }

  /**
   * [plugin] - plugin whose optional dependencies are resolved (plugin.xml, then someOptional.xml, ...)
   */
  private fun resolveOptionalDependencies(
    plugin: PluginCreator,
    pluginFile: Path,
    resourceResolver: ResourceResolver,
    problemResolver: PluginCreationResultResolver,
    dependencyChain: DependencyChain
  ) {
    if (!dependencyChain.extend(plugin)) {
      return
    }
    for ((pluginDependency, configurationFile) in plugin.optionalDependenciesConfigFiles) {
      if (dependencyChain.detectCycle(configurationFile)) {
        return
      }

      val optionalDependencyCreator = pluginLoader.load(PluginMetadataResolutionContext(pluginFile, configurationFile, false, resourceResolver, problemResolver, plugin))
      if (optionalDependencyCreator.isSuccess) {
        val optionalPlugin = optionalDependencyCreator.plugin
        plugin.plugin.optionalDescriptors += OptionalPluginDescriptor(pluginDependency, optionalPlugin, configurationFile)
        plugin.mergeContent(optionalPlugin)
      } else {
        plugin.registerProblem(OptionalDependencyDescriptorResolutionProblem(pluginDependency.id, configurationFile, optionalDependencyCreator.resolvedProblems))
      }
      resolveOptionalDependencies(optionalDependencyCreator, pluginFile, resourceResolver, problemResolver, dependencyChain)
    }
    dependencyChain.dropLast()
  }

  internal class DependencyChain {
    private val chain = LinkedList<String>()

    private val _dependencyCycles: MutableList<DependencyCycle> = mutableListOf()

    val cycles: List<DependencyCycle>
      get() = _dependencyCycles

    private val visitedPluginDescriptors = mutableSetOf<String>()

    fun detectCycle(configurationFile: String): Boolean {
      if (chain.contains(configurationFile)) {
        _dependencyCycles.add(chain + configurationFile)
        return true
      }
      return false
    }

    fun extend(plugin: PluginCreator): Boolean {
      val descriptor = plugin.descriptorPath
      if (visitedPluginDescriptors.contains(descriptor)) {
        return false
      }
      chain.addLast(descriptor)
      return true
    }

    fun dropLast() {
      chain.removeLast()
    }
  }
}
