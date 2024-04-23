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
    val dependencyCycles = DependencyCycles()
    resolveOptionalDependencies(plugin, mutableSetOf(), LinkedList(), pluginFile, resourceResolver, problemResolver, dependencyCycles)
    dependencyCycles.forEach {
      plugin.registerOptionalDependenciesConfigurationFilesCycleProblem(it)
    }
  }

  /**
   * [mainPlugin] - the root plugin (plugin.xml)
   * [currentPlugin] - plugin whose optional dependencies are resolved (plugin.xml, then someOptional.xml, ...)
   */
  private fun resolveOptionalDependencies(
    currentPlugin: PluginCreator,
    visitedConfigurationFiles: MutableSet<String>,
    path: LinkedList<String>,
    pluginFile: Path,
    resourceResolver: ResourceResolver,
    problemResolver: PluginCreationResultResolver,
    dependencyCycles: DependencyCycles
  ) {
    if (!visitedConfigurationFiles.add(currentPlugin.descriptorPath)) {
      return
    }
    path.addLast(currentPlugin.descriptorPath)
    val optionalDependenciesConfigFiles: Map<PluginDependency, String> = currentPlugin.optionalDependenciesConfigFiles
    for ((pluginDependency, configurationFile) in optionalDependenciesConfigFiles) {
      if (path.contains(configurationFile)) {
        dependencyCycles.add(path + configurationFile)
        return
      }

      val optionalDependencyCreator = pluginLoader.load(PluginMetadataResolutionContext(pluginFile, configurationFile, false, resourceResolver, problemResolver, currentPlugin))
      currentPlugin.addOptionalDescriptor(pluginDependency, configurationFile, optionalDependencyCreator)
      resolveOptionalDependencies(optionalDependencyCreator, visitedConfigurationFiles, path, pluginFile, resourceResolver, problemResolver, dependencyCycles)
    }
    path.removeLast()
  }

  internal class DependencyCycles: Iterable<DependencyCycle> {
    private val cycles: MutableList<DependencyCycle> = mutableListOf()

    fun add(cycleChain: DependencyCycle) {
     cycles.add(cycleChain)
    }

    override fun iterator(): Iterator<DependencyCycle> = cycles.iterator()
  }
}

typealias DependencyCycle = List<String>