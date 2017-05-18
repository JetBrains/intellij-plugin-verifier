package com.jetbrains.pluginverifier.output

import com.intellij.structure.plugin.PluginDependency
import com.jetbrains.pluginverifier.api.Result

/**
 * @author Sergey Patrikeev
 */
interface Printer {
  fun printResults(results: List<Result>, options: PrinterOptions)
}

data class PrinterOptions(private val ignoreAllMissingOptionalDeps: Boolean, val ignoreMissingOptionalDeps: List<String>) {

  fun ignoreMissingOptionalDependency(dependency: PluginDependency): Boolean = ignoreAllMissingOptionalDeps || ignoreMissingOptionalDeps.any { it == dependency.id }

}