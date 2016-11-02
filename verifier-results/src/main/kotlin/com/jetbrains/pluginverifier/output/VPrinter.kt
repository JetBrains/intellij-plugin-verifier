package com.jetbrains.pluginverifier.output

import com.intellij.structure.domain.PluginDependency
import com.jetbrains.pluginverifier.api.VResults

/**
 * @author Sergey Patrikeev
 */
interface VPrinter {
  fun printResults(results: VResults, options: VPrinterOptions)
}

data class VPrinterOptions(private val ignoreAllMissingOptionalDeps: Boolean, val ignoreMissingOptionalDeps: List<String>) {

  fun ignoreMissingOptionalDependency(dependency: PluginDependency): Boolean = ignoreAllMissingOptionalDeps || ignoreMissingOptionalDeps.any { it == dependency.id }

}