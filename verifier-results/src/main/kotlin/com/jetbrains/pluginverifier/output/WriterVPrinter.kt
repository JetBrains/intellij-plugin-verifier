package com.jetbrains.pluginverifier.output

import com.intellij.structure.ide.IdeVersion
import com.jetbrains.pluginverifier.api.Result
import com.jetbrains.pluginverifier.api.Verdict
import com.jetbrains.pluginverifier.dependencies.MissingDependency
import com.jetbrains.pluginverifier.misc.pluralize
import com.jetbrains.pluginverifier.problems.Problem
import java.io.PrintWriter

class WriterVPrinter(private val out: PrintWriter) : Printer {

  override fun printResults(results: List<Result>, options: PrinterOptions) {
    results.forEach { (plugin, ideVersion, verdict) ->
      val idAndVersion = "${plugin.pluginId}:${plugin.version}"
      when (verdict) {
        is Verdict.OK -> out.println("With IDE #$ideVersion the plugin $idAndVersion is OK")
        is Verdict.Warnings -> out.println("With IDE #$ideVersion the plugin $idAndVersion has ${verdict.warnings.size} warnings: ${verdict.warnings.joinToString(separator = "\n", prefix = "    ")}")
        is Verdict.Problems -> printProblems(ideVersion, idAndVersion, verdict.problems)
        is Verdict.MissingDependencies -> printMissingDependencies(options, verdict, ideVersion, idAndVersion)
        is Verdict.Bad -> out.println("The plugin $idAndVersion is broken: ${verdict.reason}")
        is Verdict.NotFound -> out.println("The plugin $idAndVersion is not found: ${verdict.reason}")
      }
    }
  }

  private fun printMissingDependencies(options: PrinterOptions, verdict: Verdict.MissingDependencies, ideVersion: IdeVersion, plugin: String) {
    val mandatoryDependencies = verdict.missingDependencies.filterNot { it.dependency.isOptional }
    printMissingMandatoryDependencies(mandatoryDependencies)

    val optionalDependencies = verdict.missingDependencies.filter { it.dependency.isOptional && !options.ignoreMissingOptionalDependency(it.dependency) }
    printMissingOptionalDependencies(optionalDependencies)

    printProblems(ideVersion, plugin, verdict.problems)
  }

  private fun printMissingMandatoryDependencies(missingMandatory: List<MissingDependency>) {
    if (missingMandatory.isNotEmpty()) {
      out.println("   Some problems might be caused by missing non-optional dependencies:")
      missingMandatory.map { it.toString() }.forEach { out.println("        $it") }
    }
  }

  private fun printMissingOptionalDependencies(missingOptional: List<MissingDependency>) {
    if (missingOptional.isNotEmpty()) {
      out.println("    Missing optional dependencies:")
      missingOptional.forEach { out.println("        ${it.dependency}: ${it.missingReason}") }
    }
  }

  private fun printProblems(ideVersion: IdeVersion, plugin: String, problems: Set<Problem>) {
    val problemsCnt = problems.size
    out.println("With IDE #$ideVersion the plugin $plugin has $problemsCnt " + "problem".pluralize(problemsCnt))
    problems.groupBy({ it.getShortDescription() }, { it.getFullDescription() }).forEach {
      out.println("    #${it.key}")
      it.value.forEach { effect ->
        out.println("        at $effect")
      }
    }
  }

}
