package com.jetbrains.pluginverifier.output.stream

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.dependencies.MissingDependency
import com.jetbrains.pluginverifier.misc.pluralize
import com.jetbrains.pluginverifier.output.ResultPrinter
import com.jetbrains.pluginverifier.output.settings.dependencies.MissingDependencyIgnoring
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.results.Result
import com.jetbrains.pluginverifier.results.Verdict
import com.jetbrains.pluginverifier.results.problems.Problem
import com.jetbrains.pluginverifier.results.warnings.Warning
import com.jetbrains.pluginverifier.tasks.InvalidPluginFile
import java.io.PrintWriter

class WriterResultPrinter(private val out: PrintWriter,
                          private val missingDependencyIgnoring: MissingDependencyIgnoring) : ResultPrinter {

  override fun printResults(results: List<Result>) {
    results.forEach { (plugin, ideVersion, verdict) ->
      return@forEach when (verdict) {
        is Verdict.OK -> out.println("With IDE #$ideVersion the plugin $plugin is OK")
        is Verdict.Warnings -> out.println("With IDE #$ideVersion the plugin $plugin has ${verdict.warnings.size} " + "warning".pluralize(verdict.warnings.size) + " : ${verdict.warnings.joinToString(separator = "\n")}")
        is Verdict.Problems -> printProblemsVerdict(ideVersion, plugin, verdict)
        is Verdict.MissingDependencies -> printMissingDependencies(verdict, ideVersion, plugin)
        is Verdict.Bad -> out.println("The plugin $plugin is broken: ${verdict.pluginProblems.joinToString()}")
        is Verdict.NotFound -> out.println("The plugin $plugin is not found: ${verdict.reason}")
        is Verdict.FailedToDownload -> out.println("The plugin $plugin is not downloaded from the Repository: ${verdict.reason}")
      }
    }
  }

  fun printInvalidPluginFiles(invalidPluginFiles: List<InvalidPluginFile>) {
    if (invalidPluginFiles.isNotEmpty()) {
      out.println("The following files specified for the verification are not valid plugins:")
      for ((pluginFile, pluginErrors) in invalidPluginFiles) {
        out.println("    $pluginFile")
        for (pluginError in pluginErrors) {
          out.println("        $pluginError")
        }
      }
    }
  }

  private fun printMissingDependencies(verdict: Verdict.MissingDependencies,
                                       ideVersion: IdeVersion,
                                       plugin: PluginInfo) {
    printDependencies(verdict)
    printWarnings(ideVersion, plugin, verdict.warnings)
    printProblems(ideVersion, plugin, verdict.problems)
  }

  private fun printDependencies(verdict: Verdict.MissingDependencies) {
    val mandatoryDependencies = verdict.directMissingDependencies.filterNot { it.dependency.isOptional }
    printMissingMandatoryDependencies(mandatoryDependencies)

    val optionalDependencies = verdict.directMissingDependencies.filter { it.dependency.isOptional && !missingDependencyIgnoring.ignoreMissingOptionalDependency(it.dependency) }
    printMissingOptionalDependencies(optionalDependencies)
  }

  private fun printProblemsVerdict(ideVersion: IdeVersion, plugin: PluginInfo, verdict: Verdict.Problems) {
    printProblems(ideVersion, plugin, verdict.problems)
    printWarnings(ideVersion, plugin, verdict.warnings)
  }

  private fun printMissingMandatoryDependencies(missingMandatory: List<MissingDependency>) {
    if (missingMandatory.isNotEmpty()) {
      out.println("   Some problems might have been caused by missing non-optional dependencies:")
      missingMandatory.map { it.toString() }.forEach { out.println("        $it") }
    }
  }

  private fun printMissingOptionalDependencies(missingOptional: List<MissingDependency>) {
    if (missingOptional.isNotEmpty()) {
      out.println("    Missing optional dependencies:")
      missingOptional.forEach { out.println("        ${it.dependency}: ${it.missingReason}") }
    }
  }

  private fun printWarnings(ideVersion: IdeVersion, plugin: PluginInfo, warnings: Set<Warning>) {
    val warningsSize = warnings.size
    out.println("With IDE #$ideVersion plugin $plugin has $warningsSize " + "warning".pluralize(warningsSize))
    warnings.forEach {
      out.println("    #${it.message}")
    }
  }

  private fun printProblems(ideVersion: IdeVersion, plugin: PluginInfo, problems: Set<Problem>) {
    val problemsCnt = problems.size
    out.println("With IDE #$ideVersion plugin $plugin has $problemsCnt " + "problem".pluralize(problemsCnt))
    problems.groupBy({ it.shortDescription }, { it.fullDescription }).forEach {
      out.println("    #${it.key}")
      it.value.forEach { fullDescription ->
        out.println("        $fullDescription")
      }
    }
  }

}
