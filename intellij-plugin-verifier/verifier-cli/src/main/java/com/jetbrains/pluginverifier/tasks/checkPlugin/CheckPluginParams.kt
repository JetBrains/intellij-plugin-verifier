package com.jetbrains.pluginverifier.tasks.checkPlugin

import com.jetbrains.plugin.structure.base.utils.closeLogged
import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.options.PluginsSet
import com.jetbrains.pluginverifier.parameters.filtering.ProblemsFilter
import com.jetbrains.pluginverifier.verifiers.packages.PackageFilter
import com.jetbrains.pluginverifier.tasks.TaskParameters
import java.nio.file.Path

class CheckPluginParams(
    val pluginsSet: PluginsSet,
    val jdkPath: Path,
    val ideDescriptors: List<IdeDescriptor>,
    val externalClassesPackageFilter: PackageFilter,
    val problemsFilters: List<ProblemsFilter>
) : TaskParameters {

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