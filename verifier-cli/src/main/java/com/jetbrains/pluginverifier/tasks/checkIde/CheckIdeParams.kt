package com.jetbrains.pluginverifier.tasks.checkIde

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.pluginverifier.dependencies.resolution.DependencyFinder
import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.misc.closeLogged
import com.jetbrains.pluginverifier.parameters.filtering.ProblemsFilter
import com.jetbrains.pluginverifier.parameters.jdk.JdkDescriptor
import com.jetbrains.pluginverifier.plugin.PluginCoordinate
import com.jetbrains.pluginverifier.repository.PluginIdAndVersion
import com.jetbrains.pluginverifier.tasks.TaskParameters


data class CheckIdeParams(val ideDescriptor: IdeDescriptor,
                          val jdkDescriptor: JdkDescriptor,
                          val pluginsToCheck: List<PluginCoordinate>,
                          val excludedPlugins: List<PluginIdAndVersion>,
                          val pluginIdsToCheckExistingBuilds: List<String>,
                          val externalClassPath: Resolver,
                          val externalClassesPrefixes: List<String>,
                          val problemsFilters: List<ProblemsFilter>,
                          val dependencyFinder: DependencyFinder) : TaskParameters {
  override fun presentableText(): String = """Check IDE configuration parameters:
IDE to be checked: $ideDescriptor
JDK: $jdkDescriptor
Plugins to be checked (${pluginsToCheck.size}): [${pluginsToCheck.joinToString()}]
Excluded plugins: [${excludedPlugins.joinToString()}]
"""

  override fun close() {
    ideDescriptor.closeLogged()
    externalClassPath.closeLogged()
  }

  override fun toString(): String = presentableText()
}