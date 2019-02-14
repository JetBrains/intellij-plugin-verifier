package com.jetbrains.pluginverifier.output.stream

import com.jetbrains.pluginverifier.VerificationTarget
import com.jetbrains.pluginverifier.dependencies.MissingDependency
import com.jetbrains.pluginverifier.misc.pluralize
import com.jetbrains.pluginverifier.output.ResultPrinter
import com.jetbrains.pluginverifier.output.settings.dependencies.MissingDependencyIgnoring
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.results.VerificationResult
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.results.structure.PluginStructureWarning
import com.jetbrains.pluginverifier.tasks.InvalidPluginFile
import java.io.PrintWriter

class WriterResultPrinter(
    private val out: PrintWriter,
    private val missingDependencyIgnoring: MissingDependencyIgnoring
) : ResultPrinter {

  override fun printResults(results: List<VerificationResult>) {
    results.forEach {
      with(it) {
        return@forEach when (this) {
          is VerificationResult.OK -> out.println("Against $verificationTarget the plugin $plugin is OK")
          is VerificationResult.StructureWarnings -> out.println("Against $verificationTarget the plugin $plugin has ${pluginStructureWarnings.size} " + "warning".pluralize(pluginStructureWarnings.size) + " : ${pluginStructureWarnings.joinToString(separator = "\n")}")
          is VerificationResult.CompatibilityProblems -> printProblemsResult(verificationTarget, plugin, this)
          is VerificationResult.MissingDependencies -> printMissingDependencies(this, verificationTarget, plugin)
          is VerificationResult.InvalidPlugin -> out.println("The plugin $plugin is broken: ${pluginStructureErrors.joinToString()}")
          is VerificationResult.NotFound -> out.println("The plugin $plugin is not found: $notFoundReason")
          is VerificationResult.FailedToDownload -> out.println("The plugin $plugin is not downloaded from the Repository: $failedToDownloadReason")
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

  private fun printMissingDependencies(
      verificationResult: VerificationResult.MissingDependencies,
      verificationTarget: VerificationTarget,
      plugin: PluginInfo
  ) {
    printDependencies(verificationResult)
    printWarnings(verificationTarget, plugin, verificationResult.pluginStructureWarnings)
    printProblems(verificationTarget, plugin, verificationResult.compatibilityProblems)
  }

  private fun printDependencies(verificationResult: VerificationResult.MissingDependencies) {
    val mandatoryDependencies = verificationResult.directMissingDependencies.filterNot { it.dependency.isOptional }
    printMissingMandatoryDependencies(mandatoryDependencies)

    val optionalDependencies = verificationResult.directMissingDependencies.filter { it.dependency.isOptional && !missingDependencyIgnoring.ignoreMissingOptionalDependency(it.dependency) }
    printMissingOptionalDependencies(optionalDependencies)
  }

  private fun printProblemsResult(
      verificationTarget: VerificationTarget,
      plugin: PluginInfo,
      verificationResult: VerificationResult.CompatibilityProblems
  ) {
    printProblems(verificationTarget, plugin, verificationResult.compatibilityProblems)
    printWarnings(verificationTarget, plugin, verificationResult.pluginStructureWarnings)
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

  private fun printWarnings(verificationTarget: VerificationTarget, plugin: PluginInfo, warnings: Set<PluginStructureWarning>) {
    val warningsSize = warnings.size
    out.println("Against $verificationTarget the plugin $plugin has $warningsSize " + "warning".pluralize(warningsSize))
    warnings.forEach {
      out.println("    #${it.message}")
    }
  }

  private fun printProblems(verificationTarget: VerificationTarget, plugin: PluginInfo, problems: Set<CompatibilityProblem>) {
    val problemsCnt = problems.size
    out.println("Against $verificationTarget the plugin $plugin has $problemsCnt " + "problem".pluralize(problemsCnt))
    problems.groupBy({ it.shortDescription }, { it.fullDescription }).forEach {
      out.println("    #${it.key}")
      it.value.forEach { fullDescription ->
        out.println("        $fullDescription")
      }
    }
  }

}
