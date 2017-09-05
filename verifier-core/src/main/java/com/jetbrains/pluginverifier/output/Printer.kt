package com.jetbrains.pluginverifier.output

import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.pluginverifier.api.Result

/**
 * @author Sergey Patrikeev
 */
interface Printer {
  fun printResults(results: List<Result>, options: PrinterOptions)
}

data class PrinterOptions(val ignoreAllMissingOptionalDeps: Boolean = false,
                          val ignoreMissingOptionalDeps: List<String> = emptyList(),
                          val needTeamCityLog: Boolean = false,
                          val teamCityGroupType: String? = null,
                          val htmlReportFile: String? = null,
                          val dumpBrokenPluginsFile: String? = null) {

  fun ignoreMissingOptionalDependency(dependency: PluginDependency): Boolean =
      ignoreAllMissingOptionalDeps || ignoreMissingOptionalDeps.any { it == dependency.id }

}