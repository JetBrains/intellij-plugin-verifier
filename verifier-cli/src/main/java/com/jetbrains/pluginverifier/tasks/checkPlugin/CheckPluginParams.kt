package com.jetbrains.pluginverifier.tasks.checkPlugin

import com.jetbrains.plugin.structure.classes.resolvers.EmptyResolver
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.misc.closeLogged
import com.jetbrains.pluginverifier.parameters.filtering.ProblemsFilter
import com.jetbrains.pluginverifier.parameters.jdk.JdkDescriptorsCache
import com.jetbrains.pluginverifier.parameters.jdk.JdkPath
import com.jetbrains.pluginverifier.tasks.PluginsToCheck
import com.jetbrains.pluginverifier.tasks.TaskParameters

class CheckPluginParams(pluginsToCheck: PluginsToCheck,
                        val jdkPath: JdkPath,
                        val ideDescriptors: List<IdeDescriptor>,
                        val jdkDescriptorsCache: JdkDescriptorsCache,
                        val externalClassesPrefixes: List<String>,
                        val problemsFilters: List<ProblemsFilter>,
                        val externalClasspath: Resolver = EmptyResolver) : TaskParameters(pluginsToCheck) {

  override fun presentableText(): String = """Check Plugin Configuration parameters:
  JDK: $jdkPath
  Plugins to be checked (${pluginsToCheck.plugins.size}): [${pluginsToCheck.plugins.joinToString()}]
  IDE builds to be checked: [${ideDescriptors.joinToString()}]
  External classes prefixes: [${externalClassesPrefixes.joinToString()}]
  """

  override fun close() {
    jdkDescriptorsCache.closeLogged()
    ideDescriptors.forEach { it.closeLogged() }
  }

  override fun toString(): String = presentableText()
}
