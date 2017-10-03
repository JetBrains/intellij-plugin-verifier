package com.jetbrains.pluginverifier.tasks

import com.jetbrains.plugin.structure.classes.resolvers.EmptyResolver
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.pluginverifier.api.IdeDescriptor
import com.jetbrains.pluginverifier.api.JdkDescriptor
import com.jetbrains.pluginverifier.filter.ProblemsFilter
import com.jetbrains.pluginverifier.misc.closeLogged
import com.jetbrains.pluginverifier.plugin.PluginCoordinate

data class CheckPluginParams(val pluginCoordinates: List<PluginCoordinate>,
                             val ideDescriptors: List<IdeDescriptor>,
                             val jdkDescriptor: JdkDescriptor,
                             val externalClassesPrefixes: List<String>,
                             val problemsFilters: List<ProblemsFilter>,
                             val externalClasspath: Resolver = EmptyResolver) : TaskParameters {

  override fun presentableText(): String = """Check Plugin Configuration parameters:
  JDK: $jdkDescriptor
  Plugins to be checked: [${pluginCoordinates.joinToString()}]
  IDE builds to be checked: [${ideDescriptors.joinToString()}]
  External classes prefixes: [${externalClassesPrefixes.joinToString()}]
  """

  override fun close() {
    ideDescriptors.forEach { it.closeLogged() }
    problemsFilters.forEach { it.closeLogged() }
  }

  override fun toString(): String = presentableText()
}
