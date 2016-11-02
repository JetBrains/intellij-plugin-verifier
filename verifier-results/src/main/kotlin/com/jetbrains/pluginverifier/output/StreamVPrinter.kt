package com.jetbrains.pluginverifier.output

import com.jetbrains.pluginverifier.api.VResult
import com.jetbrains.pluginverifier.api.VResults
import java.io.PrintStream

class StreamVPrinter(private val out: PrintStream) : VPrinter {

  override fun printResults(results: VResults, options: VPrinterOptions) {
    results.results.forEach {
      val ideVersion = it.ideDescriptor.ideVersion
      val plugin = "${it.pluginDescriptor.pluginId}:${it.pluginDescriptor.version}"
      when (it) {
        is VResult.Nice -> out.println("With the $ideVersion the plugin $plugin is OK")
        is VResult.Problems -> {
          val problemsCnt = it.problems.keySet().size
          out.println("With the $ideVersion the plugin $plugin has $problemsCnt problems:")
          it.problems.asMap().forEach {
            out.println("    #${it.key.getDescription()}")
            it.value.forEach {
              out.println("        at $it")
            }
          }
          it.dependenciesGraph.getMissingNonOptionalDependencies().apply {
            if (this.isNotEmpty()) {
              out.println("   Some problems may be caused by missing non-optional dependencies:")
              this.map { it.toString() }.forEach { out.println("        $it") }
            }
          }
          val filtered = it.dependenciesGraph.getMissingOptionalDependencies().filterKeys { !options.ignoreMissingOptionalDependency(it) }
          if (filtered.isNotEmpty()) {
            out.println("    Missing optional dependencies:")
            filtered.forEach {
              out.println("        ${it.key.id}: ${it.value.reason}")
            }
          }
        }
        is VResult.BadPlugin -> out.println("With the $ideVersion it is broken ${it.reason}")
        is VResult.NotFound -> out.println("The plugin $plugin is not found in the Repository: ${it.reason}")
      }
    }
  }

}
