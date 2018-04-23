package com.jetbrains.pluginverifier.tasks.checkPlugin

import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.misc.closeLogged
import com.jetbrains.pluginverifier.options.PluginsSet
import com.jetbrains.pluginverifier.parameters.filtering.ProblemsFilter
import com.jetbrains.pluginverifier.parameters.jdk.JdkPath
import com.jetbrains.pluginverifier.tasks.InvalidPluginFile
import com.jetbrains.pluginverifier.tasks.TaskParameters

class CheckPluginParams(pluginsSet: PluginsSet,
                        val jdkPath: JdkPath,
                        val ideDescriptors: List<IdeDescriptor>,
                        val externalClassesPrefixes: List<String>,
                        val problemsFilters: List<ProblemsFilter>,
                        val invalidPluginFiles: List<InvalidPluginFile>) : TaskParameters(pluginsSet) {

  override val presentableText
    get() = """
      |JDK              : $jdkPath
      |IDEs             : [${ideDescriptors.joinToString()}]
      |External classes : [${externalClassesPrefixes.joinToString()}]
      |$pluginsSet
    """.trimMargin()

  override fun close() {
    ideDescriptors.forEach { it.closeLogged() }
  }

}