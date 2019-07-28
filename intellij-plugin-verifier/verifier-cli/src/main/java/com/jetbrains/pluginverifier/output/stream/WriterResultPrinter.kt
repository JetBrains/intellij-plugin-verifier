package com.jetbrains.pluginverifier.output.stream

import com.jetbrains.plugin.structure.base.utils.pluralize
import com.jetbrains.pluginverifier.*
import com.jetbrains.pluginverifier.output.ResultPrinter
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.tasks.InvalidPluginFile
import com.jetbrains.pluginverifier.warnings.CompatibilityWarning
import java.io.PrintWriter

class WriterResultPrinter(private val out: PrintWriter) : ResultPrinter {

  override fun printResults(results: List<PluginVerificationResult>) {
    results.forEach { result ->
      with(result) {
        return@forEach when (this) {
          is PluginVerificationResult.Verified -> when {
            hasCompatibilityWarnings -> out.println("Against $verificationTarget the plugin $plugin has ${compatibilityWarnings.size} " + "warning".pluralize(compatibilityWarnings.size) + " : ${compatibilityWarnings.sortedBy { it.message }.joinToString(separator = "\n")}")
            hasCompatibilityProblems -> printProblemsResult(verificationTarget, plugin, this)
            hasDirectMissingDependencies -> printMissingDependencies(this, verificationTarget, plugin)
            else -> out.println("Against $verificationTarget the plugin $plugin is OK")
          }
          is PluginVerificationResult.InvalidPlugin -> out.println("The plugin $plugin is broken: ${pluginStructureErrors.joinToString()}")
          is PluginVerificationResult.NotFound -> out.println("The plugin $plugin is not found: $notFoundReason")
          is PluginVerificationResult.FailedToDownload -> out.println("The plugin $plugin is not downloaded from the Repository: $failedToDownloadReason")
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
      verificationResult: PluginVerificationResult.Verified,
      verificationTarget: PluginVerificationTarget,
      plugin: PluginInfo
  ) {
    if (verificationResult.directMissingDependencies.isNotEmpty()) {
      out.println("    Some problems might have been caused by missing dependencies:")
      for (missingDependency in verificationResult.directMissingDependencies) {
        out.println("        ${missingDependency.dependency}: ${missingDependency.missingReason}")
      }
    }
    printWarnings(verificationTarget, plugin, verificationResult.compatibilityWarnings)
    printProblems(verificationTarget, plugin, verificationResult.compatibilityProblems)
  }

  private fun printProblemsResult(
      verificationTarget: PluginVerificationTarget,
      plugin: PluginInfo,
      verificationResult: PluginVerificationResult.Verified
  ) {
    printProblems(verificationTarget, plugin, verificationResult.compatibilityProblems)
    printWarnings(verificationTarget, plugin, verificationResult.compatibilityWarnings)
  }

  private fun printWarnings(verificationTarget: PluginVerificationTarget, plugin: PluginInfo, warnings: Set<CompatibilityWarning>) {
    val warningsSize = warnings.size
    out.println("Against $verificationTarget the plugin $plugin has $warningsSize " + "warning".pluralize(warningsSize))
    warnings.sortedBy { it.message }.forEach {
      out.println("    #${it.message}")
    }
  }

  private fun printProblems(verificationTarget: PluginVerificationTarget, plugin: PluginInfo, problems: Set<CompatibilityProblem>) {
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
