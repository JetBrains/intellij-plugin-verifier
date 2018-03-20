package com.jetbrains.pluginverifier.output.html

import com.google.common.io.Resources
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.dependencies.MissingDependency
import com.jetbrains.pluginverifier.misc.HtmlBuilder
import com.jetbrains.pluginverifier.misc.VersionComparatorUtil
import com.jetbrains.pluginverifier.misc.create
import com.jetbrains.pluginverifier.misc.pluralize
import com.jetbrains.pluginverifier.output.ResultPrinter
import com.jetbrains.pluginverifier.output.settings.dependencies.MissingDependencyIgnoring
import com.jetbrains.pluginverifier.results.VerificationResult
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.results.structure.PluginStructureWarning
import java.io.PrintWriter
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path

class HtmlResultPrinter(val ideVersion: IdeVersion,
                        private val htmlFile: Path,
                        private val missingDependencyIgnoring: MissingDependencyIgnoring) : ResultPrinter {

  override fun printResults(results: List<VerificationResult>) {
    PrintWriter(Files.newBufferedWriter(htmlFile.create())).use {
      val htmlBuilder = HtmlBuilder(it)
      htmlBuilder.doPrintResults(results)
    }
  }

  private fun HtmlBuilder.doPrintResults(results: List<VerificationResult>) {
    html {
      head {
        title("Verification result of IDE $ideVersion")
        script(src = "https://ajax.aspnetcdn.com/ajax/jQuery/jquery-1.9.1.min.js", type = "text/javascript")
        script(src = "https://code.jquery.com/ui/1.9.2/jquery-ui.min.js", type = "text/javascript")
        link(rel = "stylesheet", href = "https://code.jquery.com/ui/1.9.2/themes/base/jquery-ui.css", type = "text/css")
        style(type = "text/css") { unsafe(loadReportCss()) }
      }
      body {
        h2 { +ideVersion.toString() }
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
            .sortedWith(compareByDescending(VersionComparatorUtil.COMPARATOR, { it.plugin.version }))
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
    if (results.any { it is VerificationResult.StructureWarnings }) {
      return "warnings"
    }
    return "pluginOk"
  }

  private fun HtmlBuilder.printPluginResult(result: VerificationResult) {
    val resultStyle = when (result) {
      is VerificationResult.OK -> "updateOk"
      is VerificationResult.StructureWarnings -> "warnings"
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
        printVerificationResult(result)
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
          is VerificationResult.StructureWarnings -> "${pluginStructureWarnings.size} " + "warning".pluralize(pluginStructureWarnings.size) + " found"
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

  private fun HtmlBuilder.printVerificationResult(result: VerificationResult) {
    printProblemsAndWarnings(result)
    if (result.ignoredProblems.isNotEmpty()) {
      printIgnoredProblems(result)
    }
  }

  private fun HtmlBuilder.printIgnoredProblems(result: VerificationResult) {
    printShortAndFullDescription("The following " + "problem".pluralize(result.ignoredProblems.size) + " " + "was".pluralize(result.ignoredProblems.size) + " ignored") {
      printProblems(result.ignoredProblems)
    }
  }

  private fun HtmlBuilder.printProblemsAndWarnings(result: VerificationResult) {
    with(result) {
      when (this) {
        is VerificationResult.OK -> +"No problems."
        is VerificationResult.StructureWarnings -> printWarnings(pluginStructureWarnings)
        is VerificationResult.CompatibilityProblems -> printProblems(compatibilityProblems)
        is VerificationResult.InvalidPlugin -> printShortAndFullDescription(pluginStructureErrors.joinToString(), result.plugin.pluginId)
        is VerificationResult.NotFound -> printShortAndFullDescription("Plugin ${result.plugin} is not found in the Repository", reason)
        is VerificationResult.FailedToDownload -> printShortAndFullDescription("Plugin ${result.plugin} is not downloaded from the Repository", reason)
        is VerificationResult.MissingDependencies -> printMissingDependenciesResult(this)
      }
    }
  }

  private fun HtmlBuilder.printMissingDependenciesResult(verificationResult: VerificationResult.MissingDependencies) {
    printProblems(verificationResult.compatibilityProblems)
    val missingDependencies = verificationResult.directMissingDependencies
    printMissingDependencies(missingDependencies.filterNot { it.dependency.isOptional })
    printMissingDependencies(missingDependencies.filter { it.dependency.isOptional && !missingDependencyIgnoring.ignoreMissingOptionalDependency(it.dependency) })
  }

  private fun HtmlBuilder.printWarnings(warnings: Set<PluginStructureWarning>) {
    p {
      warnings.forEach {
        +it.toString()
        br()
      }
    }
  }

  private fun HtmlBuilder.printMissingDependencies(nonOptionals: List<MissingDependency>) {
    nonOptionals.forEach { printShortAndFullDescription("missing dependency: $it", it.missingReason) }
  }

  private fun loadReportScript() = Resources.toString(HtmlResultPrinter::class.java.getResource("/reportScript.js"), Charset.forName("UTF-8"))

  private fun loadReportCss() = Resources.toString(HtmlResultPrinter::class.java.getResource("/reportCss.css"), Charset.forName("UTF-8"))

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


