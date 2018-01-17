package com.jetbrains.pluginverifier.tasks.checkIde

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.pluginverifier.dependencies.resolution.DependencyFinder
import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.misc.closeLogged
import com.jetbrains.pluginverifier.parameters.filtering.PluginIdAndVersion
import com.jetbrains.pluginverifier.parameters.filtering.ProblemsFilter
import com.jetbrains.pluginverifier.parameters.jdk.JdkDescriptorsCache
import com.jetbrains.pluginverifier.parameters.jdk.JdkPath
import com.jetbrains.pluginverifier.tasks.PluginsToCheck
import com.jetbrains.pluginverifier.tasks.TaskParameters


class CheckIdeParams(pluginsToCheck: PluginsToCheck,
                     val jdkPath: JdkPath,
                     val ideDescriptor: IdeDescriptor,
                     val jdkDescriptorsCache: JdkDescriptorsCache,
                     val excludedPlugins: List<PluginIdAndVersion>,
                     val pluginIdsToCheckExistingBuilds: List<String>,
                     val externalClassPath: Resolver,
                     val externalClassesPrefixes: List<String>,
                     val problemsFilters: List<ProblemsFilter>,
                     val dependencyFinder: DependencyFinder) : TaskParameters(pluginsToCheck) {
  override fun presentableText(): String = """Check IDE configuration parameters:
IDE to be checked: $ideDescriptor
JDK: $jdkPath
Plugins to be checked (${pluginsToCheck.plugins.size}): [${pluginsToCheck.plugins.joinToString()}]
Excluded plugins: [${excludedPlugins.joinToString()}]
"""

  override fun close() {
    ideDescriptor.closeLogged()
    jdkDescriptorsCache.closeLogged()
    externalClassPath.closeLogged()
  }

  override fun toString(): String = presentableText()
}