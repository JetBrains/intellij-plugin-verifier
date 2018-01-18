package com.jetbrains.pluginverifier.output.stream

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.dependencies.MissingDependency
import com.jetbrains.pluginverifier.misc.pluralize
import com.jetbrains.pluginverifier.output.ResultPrinter
import com.jetbrains.pluginverifier.output.settings.dependencies.MissingDependencyIgnoring
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.results.VerificationResult
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.tasks.InvalidPluginFile
import java.io.PrintWriter

class WriterResultPrinter(private val out: PrintWriter,
                          private val missingDependencyIgnoring: MissingDependencyIgnoring) : ResultPrinter {

  override fun printResults(results: List<VerificationResult>) {
    results.forEach {
      with(it) {
        return@forEach when (this) {
          is VerificationResult.OK -> out.println("With IDE #$ideVersion the plugin $plugin is OK")
          is VerificationResult.Warnings -> out.println("With IDE #$ideVersion the plugin $plugin has ${warnings.size} " + "warning".pluralize(warnings.size) + " : ${warnings.joinToString(separator = "\n")}")
          is VerificationResult.Problems -> printProblemsResult(ideVersion, plugin, this)
          is VerificationResult.MissingDependencies -> printMissingDependencies(this, ideVersion, plugin)
          is VerificationResult.InvalidPlugin -> out.println("The plugin $plugin is broken: ${pluginProblems.joinToString()}")
          is VerificationResult.NotFound -> out.println("The plugin $plugin is not found: $reason")
          is VerificationResult.FailedToDownload -> out.println("The plugin $plugin is not downloaded from the Repository: $reason")
        }
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

  private fun printMissingDependencies(verificationResult: VerificationResult.MissingDependencies,
                                       ideVersion: IdeVersion,
                                       plugin: PluginInfo) {
    printDependencies(verificationResult)
    printWarnings(ideVersion, plugin, verificationResult.warnings)
    printProblems(ideVersion, plugin, verificationResult.problems)
  }

  private fun printDependencies(verificationResult: VerificationResult.MissingDependencies) {
    val mandatoryDependencies = verificationResult.directMissingDependencies.filterNot { it.dependency.isOptional }
    printMissingMandatoryDependencies(mandatoryDependencies)

    val optionalDependencies = verificationResult.directMissingDependencies.filter { it.dependency.isOptional && !missingDependencyIgnoring.ignoreMissingOptionalDependency(it.dependency) }
    printMissingOptionalDependencies(optionalDependencies)
  }

  private fun printProblemsResult(ideVersion: IdeVersion,
                                  plugin: PluginInfo,
                                  verificationResult: VerificationResult.Problems) {
    printProblems(ideVersion, plugin, verificationResult.problems)
    printWarnings(ideVersion, plugin, verificationResult.warnings)
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

  private fun printWarnings(ideVersion: IdeVersion, plugin: PluginInfo, warnings: Set<PluginProblem>) {
    val warningsSize = warnings.size
    out.println("With IDE #$ideVersion plugin $plugin has $warningsSize " + "warning".pluralize(warningsSize))
    warnings.forEach {
      out.println("    #${it.message}")
    }
  }

  private fun printProblems(ideVersion: IdeVersion, plugin: PluginInfo, problems: Set<CompatibilityProblem>) {
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
