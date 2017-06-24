package com.jetbrains.pluginverifier.configurations

import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.*
import com.jetbrains.pluginverifier.misc.closeLogged

data class CheckPluginParams(val pluginCoordinates: List<PluginCoordinate>,
                             val ideDescriptors: List<IdeDescriptor>,
                             val jdkDescriptor: JdkDescriptor,
                             val externalClassesPrefixes: List<String>,
                             val problemsFilter: ProblemsFilter,
                             val externalClasspath: Resolver = Resolver.getEmptyResolver(),
                             val progress: Progress = DefaultProgress()) : TaskParameters {

  override fun presentableText(): String = """Check Plugin Configuration parameters:
  JDK: $jdkDescriptor
  Plugins to be checked: [${pluginCoordinates.joinToString()}]
  IDE builds to be checked: [${ideDescriptors.joinToString()}]
  External classes prefixes: [${externalClassesPrefixes.joinToString()}]
  """

  override fun close() {
    ideDescriptors.forEach { it.closeLogged() }
  }

  override fun toString(): String = presentableText()
}
