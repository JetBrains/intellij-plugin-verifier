package com.jetbrains.pluginverifier.tasks.checkPluginApi

import com.jetbrains.plugin.structure.base.utils.closeLogged
import com.jetbrains.pluginverifier.options.PluginsSet
import com.jetbrains.pluginverifier.filtering.ProblemsFilter
import com.jetbrains.pluginverifier.verifiers.packages.PackageFilter
import com.jetbrains.pluginverifier.plugin.PluginDetails
import com.jetbrains.pluginverifier.tasks.TaskParameters
import java.nio.file.Path

class CheckPluginApiParams(
    val pluginsSet: PluginsSet,
    val basePluginDetails: PluginDetails,
    val newPluginDetails: PluginDetails,
    val jdkPath: Path,
    val problemsFilters: List<ProblemsFilter>,
    val basePluginPackageFilter: PackageFilter
) : TaskParameters {

  override val presentableText
    get() = """
      |JDK              : $jdkPath
      |$pluginsSet
    """.trimMargin()

  override fun close() {
    basePluginDetails.closeLogged()
    newPluginDetails.closeLogged()
  }

}