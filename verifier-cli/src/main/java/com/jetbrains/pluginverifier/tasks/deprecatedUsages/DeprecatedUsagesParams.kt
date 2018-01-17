package com.jetbrains.pluginverifier.tasks.deprecatedUsages

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.dependencies.resolution.DependencyFinder
import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.misc.closeLogged
import com.jetbrains.pluginverifier.parameters.jdk.JdkDescriptorsCache
import com.jetbrains.pluginverifier.parameters.jdk.JdkPath
import com.jetbrains.pluginverifier.tasks.PluginsToCheck
import com.jetbrains.pluginverifier.tasks.TaskParameters


class DeprecatedUsagesParams(pluginsToCheck: PluginsToCheck,
                             val jdkPath: JdkPath,
                             val ideDescriptor: IdeDescriptor,
                             val jdkDescriptorsCache: JdkDescriptorsCache,
                             val dependencyFinder: DependencyFinder,
                             val ideVersionForCompatiblePlugins: IdeVersion) : TaskParameters(pluginsToCheck) {
  override fun presentableText(): String = """Deprecated usages detection parameters:
IDE to check: $ideDescriptor
JDK: $jdkPath
Plugins to check (${pluginsToCheck.plugins.size}): [${pluginsToCheck.plugins.joinToString()}]
"""

  override fun close() {
    ideDescriptor.closeLogged()
    jdkDescriptorsCache.closeLogged()
  }

  override fun toString(): String = presentableText()
}