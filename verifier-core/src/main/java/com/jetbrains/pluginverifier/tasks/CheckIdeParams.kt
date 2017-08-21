package com.jetbrains.pluginverifier.tasks

import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.IdeDescriptor
import com.jetbrains.pluginverifier.api.JdkDescriptor
import com.jetbrains.pluginverifier.api.PluginCoordinate
import com.jetbrains.pluginverifier.api.ProblemsFilter
import com.jetbrains.pluginverifier.dependencies.DependencyResolver
import com.jetbrains.pluginverifier.misc.closeLogged


data class CheckIdeParams(val ideDescriptor: IdeDescriptor,
                          val jdkDescriptor: JdkDescriptor,
                          val pluginsToCheck: List<PluginCoordinate>,
                          val excludedPlugins: List<PluginIdAndVersion>,
                          val pluginIdsToCheckExistingBuilds: List<String>,
                          val externalClassPath: Resolver,
                          val externalClassesPrefixes: List<String>,
                          val problemsFilter: ProblemsFilter,
                          val dependencyResolver: DependencyResolver? = null) : TaskParameters {
  override fun presentableText(): String = """Check IDE configuration parameters:
IDE to be checked: $ideDescriptor
JDK: $jdkDescriptor
Plugins to be checked: [${pluginsToCheck.joinToString()}]
Excluded plugins: [${excludedPlugins.joinToString()}]
"""

  override fun close() {
    ideDescriptor.closeLogged()
    externalClassPath.closeLogged()
  }

  override fun toString(): String = presentableText()
}