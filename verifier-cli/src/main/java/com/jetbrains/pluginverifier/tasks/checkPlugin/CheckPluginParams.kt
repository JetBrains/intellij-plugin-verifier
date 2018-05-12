package com.jetbrains.pluginverifier.tasks.checkPlugin

import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.misc.closeLogged
import com.jetbrains.pluginverifier.options.PluginsSet
import com.jetbrains.pluginverifier.parameters.filtering.ProblemsFilter
import com.jetbrains.pluginverifier.parameters.jdk.JdkPath
import com.jetbrains.pluginverifier.parameters.packages.PackageFilter
import com.jetbrains.pluginverifier.tasks.TaskParameters

class CheckPluginParams(pluginsSet: PluginsSet,
                        val jdkPath: JdkPath,
                        val ideDescriptors: List<IdeDescriptor>,
                        val externalClassesPackageFilter: PackageFilter,
                        val problemsFilters: List<ProblemsFilter>) : TaskParameters(pluginsSet) {

  override val presentableText
    get() = """
      |JDK              : $jdkPath
      |IDEs             : [${ideDescriptors.joinToString()}]
      |$pluginsSet
    """.trimMargin()

  override fun close() {
    ideDescriptors.forEach { it.closeLogged() }
  }

}