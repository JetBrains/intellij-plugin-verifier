package com.jetbrains.pluginverifier.tasks.deprecatedUsages

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.dependencies.resolution.DependencyFinder
import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.misc.closeLogged
import com.jetbrains.pluginverifier.options.PluginsSet
import com.jetbrains.pluginverifier.parameters.jdk.JdkPath
import com.jetbrains.pluginverifier.tasks.TaskParameters


class DeprecatedUsagesParams(
    pluginsSet: PluginsSet,
    val jdkPath: JdkPath,
    val ideDescriptor: IdeDescriptor,
    val dependencyFinder: DependencyFinder,
    val ideVersionForCompatiblePlugins: IdeVersion
) : TaskParameters(pluginsSet) {

  override val presentableText
    get() = """
      |IDE : $ideDescriptor
      |JDK : $jdkPath
      |$pluginsSet
    """.trimMargin()

  override fun close() {
    ideDescriptor.closeLogged()
  }

}