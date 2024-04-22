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
  /**
   * [mainPlugin] - the root plugin (plugin.xml)
   * [currentPlugin] - plugin whose optional dependencies are resolved (plugin.xml, then someOptional.xml, ...)
   */
  fun resolveOptionalDependencies(
    currentPlugin: PluginCreator,
    visitedConfigurationFiles: MutableSet<String>,
    path: LinkedList<String>,
    pluginFile: Path,
    resourceResolver: ResourceResolver,
    problemResolver: PluginCreationResultResolver,
    mainPlugin: PluginCreator,
  ) {
    if (!visitedConfigurationFiles.add(currentPlugin.descriptorPath)) {
      return
    }
    path.addLast(currentPlugin.descriptorPath)
    val optionalDependenciesConfigFiles: Map<PluginDependency, String> = currentPlugin.optionalDependenciesConfigFiles
    for ((pluginDependency, configurationFile) in optionalDependenciesConfigFiles) {
      if (path.contains(configurationFile)) {
        val configurationFilesCycle: MutableList<String> = ArrayList(path)
        configurationFilesCycle.add(configurationFile)
        mainPlugin.registerOptionalDependenciesConfigurationFilesCycleProblem(configurationFilesCycle)
        return
      }

      val optionalDependencyCreator = pluginLoader.load(PluginMetadataResolutionContext(pluginFile, configurationFile, false, resourceResolver, problemResolver, currentPlugin))
      currentPlugin.addOptionalDescriptor(pluginDependency, configurationFile, optionalDependencyCreator)
      resolveOptionalDependencies(optionalDependencyCreator, visitedConfigurationFiles, path, pluginFile, resourceResolver, problemResolver, mainPlugin)
    }
    path.removeLast()
  }


}