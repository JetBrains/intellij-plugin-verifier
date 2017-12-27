package com.jetbrains.pluginverifier.tasks.deprecatedUsages

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.dependencies.resolution.DependencyFinder
import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.parameters.jdk.JdkDescriptor
import com.jetbrains.pluginverifier.plugin.PluginCoordinate
import com.jetbrains.pluginverifier.tasks.TaskParameters


data class DeprecatedUsagesParams(val ideDescriptor: IdeDescriptor,
                                  val jdkDescriptor: JdkDescriptor,
                                  val pluginsToCheck: List<PluginCoordinate>,
                                  val dependencyFinder: DependencyFinder,
                                  val ideVersionForCompatiblePlugins: IdeVersion) : TaskParameters {
  override fun presentableText(): String = """Deprecated usages detection parameters:
IDE to check: $ideDescriptor
JDK: $jdkDescriptor
Plugins to check (${pluginsToCheck.size}): [${pluginsToCheck.joinToString()}]
"""

  override fun close() {
    ideDescriptor.close()
  }

  override fun toString(): String = presentableText()
}