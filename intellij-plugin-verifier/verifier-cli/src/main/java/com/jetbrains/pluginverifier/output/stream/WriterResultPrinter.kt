package com.jetbrains.pluginverifier.output.stream

import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.output.ResultPrinter
import com.jetbrains.pluginverifier.tasks.InvalidPluginFile
import com.jetbrains.pluginverifier.dymamic.DynamicPluginStatus
import java.io.PrintWriter

class WriterResultPrinter(private val out: PrintWriter) : ResultPrinter {

  override fun printResults(results: List<PluginVerificationResult>) {
    results.forEach { result ->
      val plugin = result.plugin
      val verificationTarget = result.verificationTarget
      out.println("Plugin $plugin against $verificationTarget: ${result.verificationVerdict}")
      if (result is PluginVerificationResult.Verified) {
        printVerificationResult(result)
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

  private fun printVerificationResult(verificationResult: PluginVerificationResult.Verified) {
    val directMissingDependencies = verificationResult.dependenciesGraph.getDirectMissingDependencies()
    if (directMissingDependencies.isNotEmpty()) {
      out.println("    Missing dependencies:")
      for (missingDependency in directMissingDependencies) {
        out.println("        ${missingDependency.dependency}: ${missingDependency.missingReason}")
      }
    }

    verificationResult.compatibilityWarnings.sortedBy { it.fullDescription }.forEach { warning ->
      out.println(warning.fullDescription.lineSequence().joinToString { "    $it" })
    }

    verificationResult.compatibilityProblems.groupBy({ it.shortDescription }, { it.fullDescription }).forEach { (shortDescription, fullDescriptions) ->
      out.println("    #$shortDescription")
      for (fullDescription in fullDescriptions) {
        out.println("        $fullDescription")
      }
    }

    when (val dynamicPluginStatus = verificationResult.dynamicPluginStatus) {
      is DynamicPluginStatus.MaybeDynamic -> out.println("        Plugin can be loaded/unloaded without IDE restart")
      is DynamicPluginStatus.NotDynamic -> out.println("        Plugin cannot be loaded/unloaded without IDE restart: " + dynamicPluginStatus.reasonsNotToLoadUnloadWithoutRestart.joinToString())
      null -> Unit
    }

  }

}
