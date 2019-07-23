package com.jetbrains.pluginverifier.output.html

import com.jetbrains.plugin.structure.base.utils.create
import com.jetbrains.plugin.structure.base.utils.pluralize
import com.jetbrains.pluginverifier.VerificationTarget
import com.jetbrains.pluginverifier.misc.HtmlBuilder
import com.jetbrains.pluginverifier.misc.VersionComparatorUtil
import com.jetbrains.pluginverifier.output.ResultPrinter
import com.jetbrains.pluginverifier.results.VerificationResult
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.results.CompatibilityWarning
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.Path

class HtmlResultPrinter(
    private val verificationTarget: VerificationTarget,
    private val htmlFile: Path
) : ResultPrinter {

  override fun printResults(results: List<VerificationResult>) {
    PrintWriter(Files.newBufferedWriter(htmlFile.create())).use {
      val htmlBuilder = HtmlBuilder(it)
      htmlBuilder.doPrintResults(results)
    }
  }

  private fun HtmlBuilder.doPrintResults(results: List<VerificationResult>) {
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

  private fun HtmlBuilder.appendPluginResults(pluginResults: List<VerificationResult>, pluginId: String) {
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

  private fun getPluginStyle(results: List<VerificationResult>): String {
    if (results.any { it is VerificationResult.CompatibilityProblems }) {
      return "pluginHasProblems"
    }
    if (results.any { it is VerificationResult.MissingDependencies }) {
      return "missingDeps"
    }
    if (results.any { it is VerificationResult.InvalidPlugin }) {
      return "badPlugin"
    }
    if (results.any { it is VerificationResult.CompatibilityWarnings }) {
      return "warnings"
    }
    return "pluginOk"
  }

  private fun HtmlBuilder.printPluginResult(result: VerificationResult) {
    val resultStyle = when (result) {
      is VerificationResult.OK -> "updateOk"
      is VerificationResult.CompatibilityWarnings -> "warnings"
      is VerificationResult.MissingDependencies -> "missingDeps"
      is VerificationResult.CompatibilityProblems -> "updateHasProblems"
      is VerificationResult.InvalidPlugin -> "badPlugin"
      is VerificationResult.NotFound -> "notFound"
      is VerificationResult.FailedToDownload -> "failedToDownload"
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

  private fun HtmlBuilder.printUpdateHeader(result: VerificationResult) {
    span(classes = "uMarker") { +"    " }
    +result.plugin.version
    small { +result.plugin.toString() }
    small {
      +with(result) {
        when (this) {
          is VerificationResult.OK -> "OK"
          is VerificationResult.CompatibilityWarnings -> "${compatibilityWarnings.size} " + "warning".pluralize(compatibilityWarnings.size) + " found"
          is VerificationResult.CompatibilityProblems -> "${compatibilityProblems.size} " + "problem".pluralize(compatibilityProblems.size) + " found"
          is VerificationResult.MissingDependencies -> "Plugin has " +
              "${directMissingDependencies.size} missing direct " + "dependency".pluralize(directMissingDependencies.size) + " and " +
              "${compatibilityProblems.size} " + "problem".pluralize(compatibilityProblems.size)
          is VerificationResult.InvalidPlugin -> "Plugin is invalid"
          is VerificationResult.NotFound -> "Plugin ${result.plugin} is not found in the Repository"
          is VerificationResult.FailedToDownload -> "Plugin ${result.plugin} is not downloaded from the Repository"
        }
      }
    }
  }

  private fun HtmlBuilder.printProblemsAndWarnings(result: VerificationResult) {
    with(result) {
      when (this) {
        is VerificationResult.OK -> +"No problems."
        is VerificationResult.CompatibilityWarnings -> printWarnings(compatibilityWarnings)
        is VerificationResult.CompatibilityProblems -> printProblems(compatibilityProblems)
        is VerificationResult.InvalidPlugin -> printShortAndFullDescription(pluginStructureErrors.joinToString(), result.plugin.pluginId)
        is VerificationResult.NotFound -> printShortAndFullDescription("Plugin ${result.plugin} is not found in the Repository", notFoundReason!!)
        is VerificationResult.FailedToDownload -> printShortAndFullDescription("Plugin ${result.plugin} is not downloaded from the Repository", failedToDownloadReason!!)
        is VerificationResult.MissingDependencies -> printMissingDependenciesResult(this)
      }
    }
  }

  private fun HtmlBuilder.printMissingDependenciesResult(verificationResult: VerificationResult.MissingDependencies) {
    printProblems(verificationResult.compatibilityProblems)
    val missingDependencies = verificationResult.directMissingDependencies
    for (missingDependency in missingDependencies) {
      printShortAndFullDescription("missing dependency: $missingDependency", missingDependency.missingReason)
    }
  }

  private fun HtmlBuilder.printWarnings(warnings: Set<CompatibilityWarning>) {
    p {
      warnings.sortedBy { it.message }.forEach {
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


