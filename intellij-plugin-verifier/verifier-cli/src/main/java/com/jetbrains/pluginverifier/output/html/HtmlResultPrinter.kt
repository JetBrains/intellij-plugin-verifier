/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.output.html

import com.jetbrains.plugin.structure.base.utils.create
import com.jetbrains.plugin.structure.ide.VersionComparatorUtil
import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.PluginVerificationTarget
import com.jetbrains.pluginverifier.misc.HtmlBuilder
import com.jetbrains.pluginverifier.output.OutputOptions
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.warnings.CompatibilityWarning
import java.io.PrintWriter
import java.nio.file.Files

class HtmlResultPrinter(
  private val verificationTarget: PluginVerificationTarget,
  private val outputOptions: OutputOptions
) {

  fun printResults(results: List<PluginVerificationResult>) {
    val reportHtmlFile = outputOptions.getTargetReportDirectory(verificationTarget).resolve("report.html")
    PrintWriter(Files.newBufferedWriter(reportHtmlFile.create())).use {
      val htmlBuilder = HtmlBuilder(it)
      htmlBuilder.doPrintResults(results)
    }
  }

  private fun HtmlBuilder.doPrintResults(results: List<PluginVerificationResult>) {
    html {
      head {
        title("Verification result $verificationTarget")
        script(src = "https://ajax.aspnetcdn.com/ajax/jQuery/jquery-1.9.1.min.js", type = "text/javascript")
        script(src = "https://code.jquery.com/ui/1.9.2/jquery-ui.min.js", type = "text/javascript")
        link(rel = "stylesheet", href = "https://code.jquery.com/ui/1.9.2/themes/base/jquery-ui.css", type = "text/css")
        style(type = "text/css") { unsafe(loadReportCss()) }
      }
      body {
        h2 { +verificationTarget.toString() }
        label {
          unsafe("""<input id="problematicOnlyCB" type="checkbox" onchange="if ($('#problematicOnlyCB').is(':checked')) {$('body').addClass('problematicOnly')} else {$('body').removeClass('problematicOnly')}">""")
          +"Show problematic plugins only"
        }
        if (results.isEmpty()) {
          +"No plugins checked"
        } else {
          results.sortedBy { it.plugin.pluginId }.groupBy { it.plugin.pluginId }.forEach { (pluginId, pluginResults) ->
            appendPluginResults(pluginResults, pluginId)
          }
        }
        script { unsafe(loadReportScript()) }
      }
    }
  }

  private fun HtmlBuilder.appendPluginResults(pluginResults: List<PluginVerificationResult>, pluginId: String) {
    div(classes = "plugin " + getPluginStyle(pluginResults)) {
      h3 {
        span(classes = "pMarker") { +"    " }
        +pluginId
      }
      div {
        pluginResults
          .sortedWith(compareByDescending(VersionComparatorUtil.COMPARATOR) { it.plugin.version })
          .forEach { printPluginResult(it) }
      }
    }
  }

  private fun getPluginStyle(results: List<PluginVerificationResult>): String {
    if (results.any { it is PluginVerificationResult.InvalidPlugin }) {
      return "badPlugin"
    }
    val verifiedResults = results.filterIsInstance<PluginVerificationResult.Verified>()
    if (verifiedResults.any { it.hasCompatibilityProblems }) {
      return "pluginHasProblems"
    }
    if (verifiedResults.any { it.hasDirectMissingMandatoryDependencies }) {
      return "missingDeps"
    }
    if (verifiedResults.any { it.hasCompatibilityWarnings }) {
      return "warnings"
    }
    return "pluginOk"
  }

  private fun HtmlBuilder.printPluginResult(result: PluginVerificationResult) {
    val resultStyle = when (result) {
      is PluginVerificationResult.Verified -> when {
        result.hasCompatibilityWarnings -> "warnings"
        result.hasDirectMissingMandatoryDependencies -> "missingDeps"
        result.hasCompatibilityProblems -> "updateHasProblems"
        else -> "updateOk"
      }
      is PluginVerificationResult.InvalidPlugin -> "badPlugin"
      is PluginVerificationResult.NotFound -> "notFound"
      is PluginVerificationResult.FailedToDownload -> "failedToDownload"
    }

    div(classes = "update $resultStyle") {
      h3 {
        printUpdateHeader(result)
      }
      div {
        printProblemsAndWarnings(result)
      }
    }
  }

  private fun HtmlBuilder.printUpdateHeader(result: PluginVerificationResult) {
    span(classes = "uMarker") { +"    " }
    +result.plugin.version
    small { +result.plugin.toString() }
    small { +result.verificationVerdict }
  }

  private fun HtmlBuilder.printProblemsAndWarnings(result: PluginVerificationResult) {
    with(result) {
      when (this) {
        is PluginVerificationResult.InvalidPlugin -> printShortAndFullDescription(pluginStructureErrors.joinToString(), result.plugin.pluginId)
        is PluginVerificationResult.NotFound -> printShortAndFullDescription("Plugin ${result.plugin} is not found in the Repository", notFoundReason)
        is PluginVerificationResult.FailedToDownload -> printShortAndFullDescription("Plugin ${result.plugin} is not downloaded from the Repository", failedToDownloadReason)
        is PluginVerificationResult.Verified -> when {
          hasCompatibilityWarnings -> printWarnings(compatibilityWarnings)
          hasCompatibilityProblems -> printProblems(compatibilityProblems)
          hasDirectMissingMandatoryDependencies -> printMissingDependenciesResult(this)
          else -> +"No problems."
        }
      }
    }
  }

  private fun HtmlBuilder.printMissingDependenciesResult(verificationResult: PluginVerificationResult.Verified) {
    printProblems(verificationResult.compatibilityProblems)
    val missingDependencies = verificationResult.directMissingMandatoryDependencies
    for (missingDependency in missingDependencies) {
      printShortAndFullDescription("missing dependency: $missingDependency", missingDependency.missingReason)
    }
  }

  private fun HtmlBuilder.printWarnings(warnings: Set<CompatibilityWarning>) {
    p {
      warnings.sortedBy { it.fullDescription }.forEach {
        +it.toString()
        br()
      }
    }
  }

  private fun loadReportScript() = HtmlResultPrinter::class.java.getResource("/reportScript.js").readText()

  private fun loadReportCss() = HtmlResultPrinter::class.java.getResource("/reportCss.css").readText()

  private fun HtmlBuilder.printProblems(problems: Set<CompatibilityProblem>) {
    problems
      .sortedBy { it.shortDescription }
      .groupBy { it.shortDescription }
      .forEach { (shortDesc, problems) ->
        val allProblems = problems.joinToString(separator = "\n") { it.fullDescription }
        printShortAndFullDescription(shortDesc, allProblems)
      }
  }

  private fun HtmlBuilder.printShortAndFullDescription(shortDescription: String, fullDescription: String) {
    printShortAndFullDescription(shortDescription) {
      +fullDescription
    }
  }

  private fun HtmlBuilder.printShortAndFullDescription(shortDescription: String, fullDescriptionBuilder: HtmlBuilder.() -> Unit) {
    div(classes = "shortDescription") {
      +shortDescription
      +" "
      a(href = "#", classes = "detailsLink") {
        +"details"
      }
      div(classes = "longDescription") {
        fullDescriptionBuilder()
      }
    }
  }

}


